(ns rollup.server.collector
  (:require [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [rollup.server.util :as u]
            [orchestra.core :as _]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [clj-commons.byte-streams :as bs]
            [aleph.tcp :as tcp]))

(s/def ::host string?)
(s/def ::port integer?)

(s/def ::output-chan ::u/chan)

(_/defn-spec start (s/keys :req [::output-chan ::u/clean-fn])
  [m (s/keys :req [::host ::port])]
  (let [c (a/chan)
        stream<?>* (atom nil)
        ;; ---
        {host ::host
         port ::port}
        m]
    (md/chain
      (tcp/client {:port port :host host})
      (fn [stream]
        (reset! stream<?>* stream)
        (md/loop []
          (md/chain
            (ms/take! stream ::drained)
            (fn [msg]
              (when-not (identical? msg ::drained)
                (a/put! c msg)
                (md/recur)))))))
    {::output-chan c
     ::u/clean-fn (fn clean! []
                    (println "Cleaning collectors")
                    (a/close! c)
                    (some-> @stream<?>* ms/close!))}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (as-> (::u/clean-fn m) func
    (func))
  nil)
