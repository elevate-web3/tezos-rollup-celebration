(ns rollup.server.webserver
  (:require [aleph.http :as http]
            [aleph.netty :as netty]
            [clojure.spec.alpha :as s]
            [orchestra.core :as _]
            [rollup.server.collector :as collector]
            [rollup.server.routes :as r]
            [rollup.server.routing :as rtng]
            [manifold.stream :as ms])
  (:import aleph.netty.AlephServer))

;; TODO: check why the spec fails on restart
(s/def ::aleph-server any? #_(isa? AlephServer %))
(s/def ::port integer?)

(_/defn-spec start-webserver! ::aleph-server
  [m (s/keys :req [::collector/output-stream]
             :req-un [::port])]
  (let [websockets* (atom #{})
        collector-stream (::collector/output-stream m)]
    (letfn [(wrap-websockets [handler]
              (fn assoc-websockets [req]
                (handler
                  (assoc req ::rtng/websockets* websockets*))))
            (wrap-collector-stream [handler]
              (fn assoc-collector-stream [req]
                (-> (assoc req ::collector/output-stream collector-stream)
                    handler)))]
      {::aleph-server (-> (r/make-ring-reitit-router)
                          wrap-websockets
                          wrap-collector-stream
                          (http/start-server (select-keys m [:port])))
       ::rtng/websockets* websockets*})))

(_/defn-spec stop-webserver! any?
  [m (s/keys :req [::aleph-server ::rtng/websockets*])]
  (let [{^java.io.Closeable server ::aleph-server
         websockets* ::rtng/websockets*} m]
    (doseq [stream @websockets*]
      (ms/close! stream))
    (.close server)))
