(ns rollup.server.aggregator
  (:require [clojure.spec.alpha :as s]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.collector :as collector]
            [rollup.server.util :as u]
            [clj-commons.byte-streams :as bs]
            [clojure.string :as str])
  (:import org.apache.commons.codec.binary.Hex))

(s/def ::output-stream ::u/stream)
(s/def ::flush-ms integer?) ;; ms period for sending data to clients

;; ??? macro for semantics and may be performance
(defmacro not-empty? [coll]
  `(boolean (seq ~coll)))

;; https://stackoverflow.com/questions/36019032/how-to-iterate-over-all-bits-of-a-byte-in-java
(defn byte->str [b]
  (-> (bit-and b 0xFF)
      Integer/toBinaryString
      (->> (format "%8s"))
      (.replace " " "0")))

(_/defn-spec start (s/keys :req [::output-stream ::u/clean-fn])
  [m (s/keys :req [::collector/output-stream ::flush-ms])]
  (println "Starting aggregator")
  (let [{collector-stream ::collector/output-stream} m
        ;; ---
        tick-stream (ms/periodically (::flush-ms m) (fn [] ::tick))
        tick-dev-tps (ms/periodically 1000 (fn [] ::tick-tps))
        merge-stream (let [stream (ms/stream)]
                       (ms/connect collector-stream stream)
                       (ms/connect tick-stream stream)
                       (ms/connect tick-dev-tps stream)
                       stream)
        sending-stream (ms/stream)
        ;; ---
        output-stream (ms/stream* {:permanent? true})]
    ;; Sending loop
    (md/future
      (md/loop []
        (md/chain
          (ms/take! sending-stream)
          (fn [byte-array-vec]
            (cond
              (nil? byte-array-vec) nil ;; stream closed
              ;; ---
              (not-empty? byte-array-vec)
              (do (->> (u/concat-byte-arrays byte-array-vec)
                       (ms/put! output-stream))
                  (md/recur))
              ;; ---
              :else (md/recur))))))
    ;; Accumulating loop
    (md/future
      (md/loop [byte-array-vec []
                tps-count 0]
        (md/chain
          (ms/take! merge-stream)
          (fn [val]
            (cond
              (identical? val ::tick)
              (do (ms/put! sending-stream byte-array-vec)
                  (md/recur [] tps-count))
              ;; ---
              (identical? val ::tick-tps)
              (do #_(println "TPS: " tps-count)
                  (ms/put! output-stream (pr-str {:tps tps-count}))
                  (md/recur byte-array-vec 0))
              ;; ---
              (u/byte-array? val)
              (md/recur (conj byte-array-vec val)
                        (inc tps-count))
              ;; stream is closed
              (nil? val) nil
              :else nil ;; TODO: check what can fail
              )))))
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

(comment

  (def trame
    [[66 -125 -59 119 64 -97 -50 50 64 -112 -60 43 65 -103 -60 103]
     [66 -118 -58 7]
     [67 -119 -50 -71]
     [65 -126 -59 -58]
     [64 -127 -55 50]
     [64 -105 -54 -88]
     [67 -107 -51 -36]])

  (->> trame
       (into []
             (comp
               (mapcat identity)
               (partition-all 4)
               ;; (map bytes->transaction)
               )))

  (->> trame
       (mapv byte-array)
       u/concat-byte-arrays)

  )
