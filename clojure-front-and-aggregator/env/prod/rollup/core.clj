(ns rollup.core
  ;; (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [juxt.clip.core :as clip]
            [rollup.system :as sys]))

(def cli-options
  [[nil "--config=" "JSON config file for collectors"
    :id ::sys/json-config-path
    :validate [#(-> % str/blank? not) "JSON config file path cannot be empty"]]])

(comment
  (let [cli-options [[nil "--config=" "JSON config file for collectors"
                      :id ::sys/json-config-path
                      :validate [#(-> % str/blank? not) "JSON config file path cannot be empty"]]]]
    (cli/parse-opts '("--config=foo/dam.json") cli-options))
  )

(defn -main
  "Launch production version of the rollup aggregator"
  [& args]
  (let [cli-config (cli/parse-opts args cli-options)
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
