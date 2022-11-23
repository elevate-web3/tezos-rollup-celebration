(ns rollup.server.webserver
  (:require [aleph.http :as http]
            [aleph.netty :as netty]
            [clojure.spec.alpha :as s]
            [orchestra.core :as _]
            [rollup.server.routes :as r]))

(s/def ::aleph-server #(satisfies? netty/AlephServer %))
(s/def ::port integer?)

(_/defn-spec start-webserver! ::aleph-server
  [data (s/keys :req-un [::port])]
  (-> (r/make-ring-reitit-router)
      (http/start-server data)))

(_/defn-spec stop-webserver! any?
  [^java.io.Closeable server ::aleph-server]
  (.close server))
