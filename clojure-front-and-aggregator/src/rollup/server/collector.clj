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

(_/defn-spec listen-collector (s/keys :req [::u/chan ::u/clean-fn])
  [m (s/keys :req [::host ::port])]
  (let [c (a/chan)
        stream<?>* (atom nil)]
    (letfn [(clean! []
              (a/close! c)
              (some-> @stream<?>* ms/close!))]
      (md/chain
        (tcp/client {:port 1234 :host "localhost"})
        (fn [stream]
          (reset! stream<?>* stream)
          (md/loop []
            (md/chain
              (ms/take! stream ::drained)
              (fn [msg]
                (when-not(identical? msg ::drained)
                  (a/put! c msg)
                  (md/recur)))))))
      {::u/chan c
       ::u/clean-fn clean!})))
