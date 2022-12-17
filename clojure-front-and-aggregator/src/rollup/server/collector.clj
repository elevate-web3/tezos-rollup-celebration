(ns rollup.server.collector
  (:require [aleph.tcp :as tcp]
            [clojure.spec.alpha :as s]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.config :as c]
            [rollup.server.util :as u]
            [rollup.shared.util :as su]))

(s/def ::host string?)
(s/def ::port integer?)
(s/def ::row integer?)
(s/def ::column integer?)

(s/def ::output-stream ::u/stream)
(s/def ::collector-stream ::u/stream)

(s/def ::cell-stream
  (s/keys :req [::row ::column ::collector-stream]))

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [configs (s/coll-of (s/keys :req [::host ::port ::row ::column]))]
  (println "Starting collectors for" (count configs) "connections")
  (let [output-stream (ms/sliding-stream c/sliding-stream-buffer-size)]
    (deref
      (md/let-flow [cell-streams (->> configs
                                      (mapv #(md/chain
                                               (tcp/client {:port (::port %)
                                                            :host (::host %)})
                                               (fn [stream]
                                                 (merge (select-keys % [::row ::column])
                                                        {::collector-stream stream}))))
                                      (apply md/zip))]
        (let [stream-count (count cell-streams)]
          (doseq [cell-stream cell-streams]
            (let [{row ::row
                   col ::column} cell-stream]
              (md/future
                (md/loop []
                  (md/chain
                    (ms/take! (::collector-stream cell-stream) ::drained)
                    (fn [msg]
                      (if (identical? msg ::drained)
                        (println "Stream of collector row:" row "col:" col "drained")
                        (md/chain
                          (->> msg
                               (into []
                                     (comp
                                       (partition-all 4)
                                       (filter #(-> (count %) (= 4))) ;; Drop potential remaining bytes
                                       (map (fn [bytes]
                                              (let [uint-array (u/concat-byte-arrays [(byte-array [row col])
                                                                                      (byte-array bytes)])]
                                                #_(println (su/bytes->transaction uint-array))
                                                uint-array)))))
                               (ms/put! output-stream))
                          (fn put-success [_] (md/recur))))))))))
          {::output-stream output-stream
           ::u/clean-fn (fn clean! []
                          (println "Cleaning" stream-count "collectors")
                          (ms/close! output-stream)
                          (doseq [cell-stream cell-streams]
                            (ms/close! (::collector-stream cell-stream))))})))))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
