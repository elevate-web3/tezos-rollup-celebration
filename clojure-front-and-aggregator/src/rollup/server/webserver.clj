(ns rollup.server.webserver
  (:require [aleph.http :as http]
            [aleph.netty :as netty]
            [clojure.spec.alpha :as s]
            [orchestra.core :as _]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.routes :as r]))

(s/def ::aleph-server #(satisfies? netty/AlephServer %))
(s/def ::port integer?)

(_/defn-spec start-webserver! ::aleph-server
  [m (s/keys :req [::aggregator/output-stream]
             :req-un [::port])]
  (-> (r/make-ring-reitit-router m)
      (http/start-server (select-keys m [:port]))))

(_/defn-spec stop-webserver! any?
  [^java.io.Closeable server ::aleph-server]
  (.close server))
