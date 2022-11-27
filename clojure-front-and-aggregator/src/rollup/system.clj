(ns rollup.system
  (:require [rollup.server.webserver :as webserver]
            [rollup.server.config :as c]
            [rollup.server.tick :as tick]))

(defn get-system-config []
  {:components
   {::webserver {:start `(webserver/start-webserver! {:port 9000})
                 :stop `webserver/stop-webserver!}}})
