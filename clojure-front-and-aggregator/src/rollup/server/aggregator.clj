(ns rollup.server.aggregator
  (:require [clojure.spec.alpha :as s]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.collector :as collector]
            [rollup.server.util :as u]))

(s/def ::output-stream ::u/stream)
(s/def ::flush-ms integer?) ;; ms period for sending data to clients

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [m (s/keys :req [::collector/output-stream ::flush-ms])]
  (let [continue?* (atom true)
        {collector-stream ::collector/output-stream} m
        ;; ---
        tick-stream (ms/periodically (::flush-ms m) (fn [] ::tick))
        merge-stream (let [stream (ms/stream)]
                       (ms/connect collector-stream stream)
                       (ms/connect tick-stream stream)
                       stream)
        ;; ---
        output-stream (ms/stream)]
    (md/loop [byte-count 0]
      (when (true? @continue?*)
        (md/chain
          (ms/take! merge-stream)
          #(cond
             (identical? % ::tick)
             (do (ms/put! output-stream byte-count)
                 (md/recur byte-count))
             ;; ---
             (u/byte-array? %)
             (md/recur (-> % count (+ byte-count)))
             ;; ---
             :else nil ;; TODO: check what can fail
             ))))
    {::u/clean-fn (fn clean []
                    (println "Cleaning aggregator")
                    (reset! continue?* false)
                    (doseq [s [output-stream tick-stream merge-stream]]
                      (ms/close! s)))
     ::output-stream output-stream}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (as-> (::u/clean-fn m) func
    (func))
  nil)
