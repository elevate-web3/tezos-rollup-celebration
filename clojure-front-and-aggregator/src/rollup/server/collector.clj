(ns rollup.server.collector
  (:require [aleph.tcp :as tcp]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.config :as c]
            [rollup.server.mockup :as mockup]
            [rollup.server.util :as u]
            [rollup.shared.util :as su])
  (:import io.netty.buffer.Unpooled))

(s/def ::host string?)
(s/def ::port integer?)
(s/def ::row integer?)
(s/def ::column integer?)

(s/def ::output-stream ::u/stream)
(s/def ::collector-stream ::u/stream)

(s/def ::cell-stream
  (s/keys :req [::row ::column ::collector-stream]))

(defn make-mockup-stream [config]
  (let [mockup-fn (case (::c/stream-mockup config)
                    "static" mockup/get-static-mockup-stream
                    "random" mockup/get-random-mockup-stream)
        vals (for [col (-> config ::c/rows parse-long range)
                   column (-> config ::c/columns parse-long range)]
               (md/success-deferred
                 {::row col
                  ::column column
                  ::collector-stream (mockup-fn
                                       {::mockup/msg-size (-> config ::c/msg-size parse-long)
                                        ::mockup/interval (-> config ::c/interval parse-long)})}))]
    (apply md/zip vals)))

(defn- make-tcp-connections [json-config-path]
  (->> (let [path json-config-path ;; alias
             file (io/as-file path)]
         (assert (.exists file) (str "The conf file " path " does not exist"))
         (io/as-file path))
       slurp
       (s/assert #(not (str/blank? %)))
       json/read-str
       (s/assert (s/coll-of map? :kind vector?))
       (mapv #(do {::host (get % "host")
                   ::port (get % "port")
                   ::row (get % "row")
                   ::column (get % "column")}))
       (s/assert (s/coll-of (s/keys :req [::host ::port ::row ::column])))
       (mapv #(md/chain
                (tcp/client {:port (::port %)
                             :host (::host %)})
                (fn [stream]
                  (merge (select-keys % [::row ::column])
                         {::collector-stream stream}))))
       (apply md/zip)))

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [options (s/keys :req [::c/options])]
  (let [m (::c/options options)
        output-stream (ms/sliding-stream c/sliding-stream-buffer-size)
        cell-streams (deref
                       (cond
                         (::c/stream-mockup m)
                         (make-mockup-stream m)
                         ;; ---
                         (::c/json-config-path m)
                         (make-tcp-connections (::c/json-config-path m))
                         ;; ---
                         :else (throw (Error. "Impossible to start the system"))))
        stream-count (count cell-streams)]
    (println "Starting collectors for" stream-count "connections")
    (doseq [cell-stream cell-streams]
      (let [{row ::row
             col ::column} cell-stream]
        (md/future
          (md/loop []
            (md/chain
              (ms/take! (::collector-stream cell-stream) ::drained)
              (fn [msg]
                ;; msg has class [B (byte buffer)
                (if (identical? msg ::drained)
                  (println "Stream of collector row:" row "col:" col "drained")
                  (md/chain
                    (->> msg
                         (partition-all 4)
                         (filter #(-> (count %) (= 4))) ;; Drop potential remaining bytes
                         (map (fn [bytes] (concat [row col] bytes)))
                         (apply concat)
                         byte-array
                         Unpooled/wrappedBuffer
                         (ms/put! output-stream))
                    (fn put-success [_] (md/recur))))))))))
    {::output-stream output-stream
     ::u/clean-fn (fn clean! []
                    (println "Cleaning" stream-count "collectors")
                    (ms/close! output-stream)
                    (doseq [cell-stream cell-streams]
                      (ms/close! (::collector-stream cell-stream))))}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
