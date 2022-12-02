(ns rollup.server.collector
  (:require [clojure.spec.alpha :as s]
            [rollup.server.util :as u]
            [orchestra.core :as _]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [aleph.tcp :as tcp]))

(s/def ::host string?)
(s/def ::port integer?)

(s/def ::output-stream ::u/stream)

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [configs (s/coll-of (s/keys :req [::host ::port]))]
  (println "Starting collector connections")
  (let [output-stream (ms/stream)]
    (-> (md/let-flow [streams (->> configs
                                   (mapv #(tcp/client {:port (::port %)
                                                       :host (::host %)}))
                                   (apply md/zip))]
          (doseq [stream streams]
            (md/future
              (md/loop []
                (md/chain
                  (ms/take! stream ::drained)
                  (fn [msg]
                    (if (identical? msg ::drained)
                      (println "Collector stream drained")
                      (do (ms/put! output-stream msg)
                          (md/recur))))))))
          {::output-stream output-stream
           ::u/clean-fn (fn clean! []
                          (println "Cleaning collectors")
                          (ms/close! output-stream)
                          (doseq [stream streams]
                            (ms/close! stream)))})
        deref)))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
