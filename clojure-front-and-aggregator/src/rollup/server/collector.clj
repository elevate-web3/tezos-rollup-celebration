(ns rollup.server.collector
  (:require [clojure.spec.alpha :as s]
            [rollup.server.util :as u]
            [orchestra.core :as _]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [aleph.tcp :as tcp]))

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
  (println "Starting collector connections")
  (let [output-stream (ms/stream)]
    (-> (md/let-flow [cell-streams (->> configs
                                        (mapv #(md/chain
                                                 (tcp/client {:port (::port %)
                                                              :host (::host %)})
                                                 (fn [stream]
                                                   (merge (select-keys % [::row ::column])
                                                          {::collector-stream stream}))))
                                        (apply md/zip))]
          (doseq [cell-stream cell-streams]
            (let [{row ::row
                   col ::column} cell-stream]
              (md/future
                (md/loop []
                  (md/chain
                    (ms/take! (::collector-stream cell-stream) ::drained)
                    (fn [msg]
                      (if (identical? msg ::drained)
                        (println "Collector stream drained")
                        (md/chain
                          (->> msg
                               (into []
                                     (comp
                                       (partition-all 4)
                                       (map (fn [bytes]
                                              (u/concat-byte-arrays [(byte-array [row col])
                                                                     (byte-array bytes)])))))
                               (ms/put-all! output-stream))
                          (fn put-all-success [_]
                            (md/recur))))))))))
          {::output-stream output-stream
           ::u/clean-fn (fn clean! []
                          (println "Cleaning collectors")
                          (ms/close! output-stream)
                          (doseq [cell-stream cell-streams]
                            (ms/close! (::collector-stream cell-stream))))})
        deref)))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
