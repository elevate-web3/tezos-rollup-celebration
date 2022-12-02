(ns rollup.server.handlers.public
  (:require [ring.util.response :as resp]
            [clojure.java.io :as io]
            [rollup.server.routing :as rtng]
            [rollup.server.aggregator :as aggregator]
            [orchestra.core :as _]
            [manifold.stream :as ms]))

(let [body (slurp (io/resource "public/index.html"))]
  (defn home-page [_req]
    (-> (resp/response body)
        (resp/content-type "text/html; charset=utf-8"))))

;; https://gist.github.com/jeroenvandijk/67d064e0bb08b900e656
(_/defn-spec data-stream ::rtng/response
  [req ::rtng/request]
  (let [source-stream (::aggregator/output-stream req)
        sse-streams* (::rtng/sse-streams* req)
        stream (ms/stream)]
    (ms/on-closed stream (fn clean []
                           (swap! sse-streams* disj stream)))
    (ms/connect source-stream stream)
    (swap! sse-streams* conj stream)
    {:status 200
     :headers {"Content-Type" "text/event-stream"
               "Cache-Control" "no-cache, no-store, max-age=0, must-revalidate"
               "Pragma" "no-cache"}
     :body stream}))
