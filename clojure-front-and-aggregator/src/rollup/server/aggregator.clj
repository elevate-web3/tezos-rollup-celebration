(ns rollup.server.aggregator
  (:require [clojure.core.async :as a]
            [rollup.server.collector :as collector]
            [rollup.server.tick :as tick]
            [rollup.server.util :as u]
            [orchestra.core :as _]
            [clojure.spec.alpha :as s]))

(s/def ::output-chan ::u/chan)

(_/defn-spec start (s/keys :req [::output-chan ::u/clean-fn])
  [m (s/keys :req [::collector/output-chan])]
  (let [continue?* (atom true)
        {collector-chan ::collector/output-chan} m
        ;; ---
        {tick-ch ::u/chan
         clean-tick ::u/clean-fn}
        (tick/start-ticking {::tick/ms-time 1000})
        ;; ---
        output-ch (a/chan)]
    (a/go-loop [byte-count 0]
      (when (true? @continue?*)
        (let [[val ch] (a/alts! [collector-chan tick-ch])]
          (if (identical? ch tick-ch)
            ;; tick event
            (do (a/put! output-ch byte-count)
                (recur byte-count))
            ;; else byte msg
            (let [byte-arr val]
              (recur (-> byte-arr u/throw-on-err count (+ byte-count))))))))
    {::u/clean-fn (fn clean []
                    (println "Cleaning aggregator")
                    (reset! continue?* false)
                    (a/close! output-ch)
                    (clean-tick))
     ::output-chan output-ch}))

(_/defn-spec stop nil?
  [m (s/keys :req [::u/clean-fn])]
  (as-> (::u/clean-fn m) func
    (func))
  nil)
