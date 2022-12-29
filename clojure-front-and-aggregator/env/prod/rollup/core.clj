(ns rollup.core
  ;; (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.cli :as cli]
            [juxt.clip.core :as clip]
            [rollup.server.config :as c]
            [rollup.system :as sys]))

(defn -main
  "Launch production version of the rollup aggregator"
  [& args]
  (let [cli-config (cli/parse-opts args c/cli-options-config)
        system-config (sys/get-system-config cli-config)
        system (clip/start system-config)]
    (println "Rollup prod aggregator launched")
    (s/check-asserts true)
    (.addShutdownHook
      (Runtime/getRuntime)
      (Thread. (fn shutdown []
                 (println "Stopping system")
                 (clip/stop system-config system))))
    @(promise)))
