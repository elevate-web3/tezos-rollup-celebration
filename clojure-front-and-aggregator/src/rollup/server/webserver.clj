(ns rollup.server.webserver
  (:require [aleph.http :as http]
            [aleph.netty :as netty]
            [clojure.spec.alpha :as s]
            [orchestra.core :as _]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.routes :as r]
            [rollup.server.routing :as rtng]
            [manifold.stream :as ms]))

(s/def ::aleph-server #(satisfies? netty/AlephServer %))
(s/def ::port integer?)

(_/defn-spec start-webserver! ::aleph-server
  [m (s/keys :req [::aggregator/output-stream]
             :req-un [::port])]
  (let [sse-streams* (atom #{})]
    (letfn [(wrap-sse-streams [handler]
              (fn assoc-sse-streams [req]
                (handler
                  (assoc req ::rtng/sse-streams* sse-streams*))))]
      {::aleph-server (-> (r/make-ring-reitit-router m)
                          wrap-sse-streams
                          (http/start-server (select-keys m [:port])))
       ::rtng/sse-streams* sse-streams*})))

(_/defn-spec stop-webserver! any?
  [m (s/keys :req [::aleph-server ::rtng/sse-streams*])]
  (let [{^java.io.Closeable server ::aleph-server
         sse-streams* ::rtng/sse-streams*} m]
    (doseq [stream @sse-streams*]
      (ms/close! stream))
    (.close server)))
