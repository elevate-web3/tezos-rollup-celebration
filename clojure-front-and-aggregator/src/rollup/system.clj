(ns rollup.system
  (:require [juxt.clip.core :as clip]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.collector :as collector]
            [rollup.server.config :as c]
            [rollup.server.tick :as tick]
            [rollup.server.webserver :as webserver]
            [rollup.server.util :as u]))

(defn get-system-config []
  {:components
   {::collectors {:start `(collector/start {::collector/host "localhost"
                                            ::collector/port 1234})
                  :stop `collector/stop}
    ::aggregator {:start `(aggregator/start (select-keys (clip/ref ::collectors) [::collector/output-chan]))
                  :stop `aggregator/stop}
    ::webserver {:start `(webserver/start-webserver! (merge {:port 9000}
                                                            (select-keys (clip/ref ::aggregator) [::aggregator/output-chan])))
                 :stop `webserver/stop-webserver!}}})
