(ns rollup.system
  (:require [juxt.clip.core :as clip]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.collector :as collector]
            [rollup.server.config :as c]
            [rollup.server.webserver :as webserver]))

(defn get-system-config [cli-options]
  (let [options (c/parse-cli-options cli-options)]
    {:components
     {::collectors {:start `(collector/start {::c/options ~options})
                    :stop `collector/stop}
      ::aggregator {:start `(aggregator/start
                              (merge {::aggregator/flush-ms 40} ;; ...
                                     (select-keys (clip/ref ::collectors) [::collector/output-stream])))
                    :stop `aggregator/stop}
      ::webserver {:start `(webserver/start-webserver!
                             (merge {:port 9000}
                                    (select-keys (clip/ref ::aggregator) [::aggregator/output-stream])))
                   :stop `webserver/stop-webserver!}}}))
