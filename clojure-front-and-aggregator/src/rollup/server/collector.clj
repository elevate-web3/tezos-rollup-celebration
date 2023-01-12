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
            [rollup.server.util :as u])
  (:import io.netty.buffer.Unpooled
           io.netty.buffer.ByteBuf))

(s/def ::host string?)
(s/def ::port integer?)

(s/def ::output-stream ::u/stream)
(s/def ::collector-stream ::u/stream)

(s/def ::cell-stream
  (s/keys :req [::collector-stream]))

(def ^:const ws-limit
  ;; (* 64 1024) ;; max
  (* 60 1024)
  ;; (* 10 1024)
  )

(defn aggregate-xf [xf]
  (let [state (volatile! (Unpooled/compositeBuffer))]
    (fn
      ([] (xf))
      ([result] (xf result))
      ([result chunk]
       (let [size (.readableBytes @state)
             chunk-size (.readableBytes chunk)]
         (when (> (+ size chunk-size) ws-limit)
           (xf result @state)
           (vreset! state (Unpooled/compositeBuffer)))
         (.addComponent @state true chunk)
         result)))))

(defn make-mockup-stream [config]
  (let [mockup-fn (case (::c/stream-mockup config)
                    "static" mockup/get-static-mockup-stream
                    "random" mockup/get-random-mockup-stream)
        vals (for [row (-> config ::c/rows parse-long range)
                   column (-> config ::c/columns parse-long range)]
               (md/success-deferred
                 {::row row
                  ::column column
                  ::collector-stream (mockup-fn
                                       {::mockup/msg-size (-> config ::c/msg-size parse-long)
                                        ::mockup/interval (-> config ::c/interval parse-long)
                                        ::mockup/row row
                                        ::mockup/column column})}))]
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
                   ::port (get % "port")}))
       (s/assert (s/coll-of (s/keys :req [::host ::port])))
       (mapv #(md/chain
                (tcp/client {:port (::port %)
                             :host (::host %)
                             :raw-stream? true})
                (fn [stream]
                  (ms/on-closed stream (fn []
                                         (println (str "Closing TCP source at "
                                                       (::host %)
                                                       ":"
                                                       (::port %)))))
                  stream)))
       (apply md/zip)))

(def ok*
  (atom 0))

(def error*
  (atom 0))

(def buf-size*
  (atom {}))

(comment

  @ok*
  @error*

  (->> @buf-size*
       (sort-by :len)
       vec)

  )

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [options (s/keys :req [::c/options])]
  (let [m (::c/options options)
        collected-stream (ms/sliding-stream c/sliding-stream-buffer-size)
        output-stream (ms/stream* {:permanent? true
                                   :buffer-size 120000000})
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
      (ms/connect cell-stream collected-stream))
    (->> collected-stream
         (ms/transform
           (map (fn [data]

                  (swap! buf-size* #(let [len (.readableBytes data)
                                          info {:len len
                                                :capacity (.capacity data)
                                                :quot (quot len 6)
                                                :mod (mod len 6)}]
                                      (if-let [n (get % info)]
                                        (assoc % info (inc n))
                                        (assoc % info 1))))
                  data)))
         ;; (ms/transform aggregate-xf)
         (ms/consume (fn [data]
                       (ms/put! output-stream data))))
    {::output-stream output-stream
     ::u/clean-fn (fn clean! []
                    (println "Cleaning" stream-count "collectors")
                    (ms/close! output-stream)
                    (ms/close! collected-stream)
                    (doseq [cell-stream cell-streams]
                      (ms/close! cell-stream)))}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
