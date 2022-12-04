(ns rollup.core
  ;; (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [juxt.clip.core :as clip]
            [rollup.system :as sys]))

(defn -main
  "Launch production version of the rollup aggregator"
  [& _args]
  (let [system-config (sys/get-system-config)
        system (clip/start system-config)]
    (println "Rollup prod aggregator launched")
    (s/check-asserts true)
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn shutdown []
                 (println "Stopping system")
                 (clip/stop system-config system))))
    @(promise)))
