(ns rollup.system
  (:require [rollup.server.webserver :as webserver]))

(defn get-system-config []
  {:components
   {::webserver {:start `(webserver/start-webserver! {:port 9000})
                 :stop `webserver/stop-webserver!}}})
