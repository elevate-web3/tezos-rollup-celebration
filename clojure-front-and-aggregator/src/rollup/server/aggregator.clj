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
  (println "Starting aggregator")
  (let [{collector-stream ::collector/output-stream} m
        ;; ---
        tick-stream (ms/periodically (::flush-ms m) (fn [] ::tick))
        merge-stream (let [stream (ms/stream)]
                       (ms/connect collector-stream stream)
                       (ms/connect tick-stream stream)
                       stream)
        ;; ---
        output-stream (ms/stream* {:permanent? true})]
    (md/loop [byte-count 0]
      (md/chain
        (ms/take! merge-stream)
        #(cond
           (identical? % ::tick)
           (do #_(println byte-count)
               (ms/put! output-stream (str "data: " byte-count "\n\n"))
               (md/recur byte-count))
           ;; ---
           (u/byte-array? %)
           (md/recur (-> % count (+ byte-count)))
           ;; stream is closed
           (nil? %) nil
           :else nil ;; TODO: check what can fail
           )))
    {::u/clean-fn (fn clean []
                    (println "Cleaning aggregator")
                    (doseq [s [merge-stream output-stream tick-stream]]
                      (ms/close! s)))
     ::output-stream output-stream}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (when-let [func (::u/clean-fn m)]
    (func))
  nil)
