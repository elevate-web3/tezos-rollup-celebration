(ns rollup.server.aggregator
  (:require [clojure.spec.alpha :as s]
            [manifold.deferred :as md]
            [byte-streams :as bs]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.collector :as collector]
            [rollup.server.config :as c]
            [rollup.server.util :as u]))

(s/def ::output-stream ::u/stream)
(s/def ::flush-ms integer?) ;; ms period for sending data to clients

;; ??? macro for semantics and may be performance
(defmacro not-empty? [coll]
  `(boolean (seq ~coll)))

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [m (s/keys :req [::collector/output-stream ::flush-ms])]
  (println "Starting aggregator")
  (let [{collector-stream ::collector/output-stream} m
        ;; ---
        tick-stream (ms/periodically (::flush-ms m) (fn [] ::tick))
        ;; tick-tps (ms/periodically 1000 (fn [] ::tick-tps))
        merge-stream (let [stream (ms/stream)]
                       (ms/connect collector-stream stream)
                       (ms/connect tick-stream stream)
                       ;; (ms/connect tick-tps stream)
                       stream)
        ;; sending-stream (ms/sliding-stream c/sliding-stream-buffer-size)
        ;; ---
        output-stream (ms/stream* {:permanent? true
                                   :buffer-size 120000000})]
    ;; Sending loop
    #_(md/loop []
      (md/chain
        (ms/take! sending-stream ::drained)
        (fn [byte-array-vec]
          (cond
            (identical? ::drained byte-array-vec) nil
            ;; ---
            (not-empty? byte-array-vec)
            (md/chain
              (md/future
                (->> byte-array-vec ms/->source))
              (fn [data] (ms/put! output-stream data))
              (fn [_] (md/recur)))
            ;; ---
            :else (md/recur)))))
    ;; Accumulating loop
    (md/loop [byte-array-list (list)]
      (-> (md/chain
            (ms/take! merge-stream ::drained)
            (fn [val]
              (cond
                (identical? val ::tick)
                (do (when-not (empty? byte-array-list)
                      (ms/put! output-stream byte-array-list))
                    (md/recur (list)))
                ;; ---
                (u/netty-buffer? val)
                (md/recur (conj byte-array-list val))
                ;; stream is closed
                (identical? val ::drained) nil
                :else (md/recur byte-array-list) ;; TODO: check what can fail
                )))
          (md/catch (fn throw-aggregator-error [e] (throw e)))))
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
