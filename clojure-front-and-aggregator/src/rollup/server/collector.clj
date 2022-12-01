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
  [m (s/keys :req [::host ::port])]
  (let [output-stream (ms/stream)
        stream<?>* (atom nil)
        {host ::host
         port ::port} m]
    (md/chain
      (tcp/client {:port port :host host})
      (fn [input-stream]
        (reset! stream<?>* input-stream)
        (md/loop []
          (md/chain
            (ms/take! input-stream ::drained)
            (fn [msg]
              (when-not (identical? msg ::drained)
                (ms/put! output-stream msg)
                (md/recur)))))))
    {::output-stream output-stream
     ::u/clean-fn (fn clean! []
                    (println "Cleaning collectors")
                    (ms/close! output-stream)
                    (some-> @stream<?>* ms/close!))}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (as-> (::u/clean-fn m) func
    (func))
  nil)
