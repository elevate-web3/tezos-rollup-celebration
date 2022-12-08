(ns rollup.system
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [juxt.clip.core :as clip]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.collector :as collector]
            [rollup.server.webserver :as webserver]))

;; Path to a custom JSON file for collectors
(s/def ::json-config-path string?)

(defn get-system-config [cli-config]
  (let [collectors (->> (if-let [path (get-in cli-config [:options ::json-config-path])]
                          (let [file (io/as-file path)]
                            (assert (.exists file) (str "The conf file " path " does not exist"))
                            (io/as-file path))
                          ;; Default config
                          (io/resource "collectors-example.json"))
                        slurp
                        (s/assert #(not (str/blank? %)))
                        json/read-str
                        (s/assert (s/coll-of map? :kind vector?))
                        (mapv #(do {::collector/host (get % "host")
                                    ::collector/port (get % "port")}))
                        (s/assert (s/coll-of (s/keys :req [::collector/host
                                                           ::collector/port]))))]
    {:components
     {::collectors {:start `(collector/start ~collectors)
                    :stop `collector/stop}
      ::aggregator {:start `(aggregator/start
                              (merge {::aggregator/flush-ms 50}
                                     (select-keys (clip/ref ::collectors) [::collector/output-stream])))
                    :stop `aggregator/stop}
      ::webserver {:start `(webserver/start-webserver!
                             (merge {:port 9000}
                                    (select-keys (clip/ref ::aggregator) [::aggregator/output-stream])))
                   :stop `webserver/stop-webserver!}}}))
