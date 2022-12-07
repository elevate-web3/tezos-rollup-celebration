(ns rollup.system
  (:require [juxt.clip.core :as clip]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.collector :as collector]
            [rollup.server.config :as c]
            [rollup.server.webserver :as webserver]
            [rollup.server.util :as u]))

(defn get-system-config []
  (let [collectors (->> (range 1234 1244)
                        (mapv #(do {::collector/host "localhost"
                                    ::collector/port %})))]
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
