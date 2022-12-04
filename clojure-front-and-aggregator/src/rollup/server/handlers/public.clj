(ns rollup.server.handlers.public
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [comb.template :as comb]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [ring.util.response :as resp]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.routing :as rtng]))

(let [template (slurp (io/resource "templates/index.html"))
      body (comb/eval template {:main-js-name (if-let [asset-file (io/resource "public/cljs/assets.edn")]
                                                (->> (some #(when (-> % :name (= :main))
                                                              (:output-name %))
                                                           (edn/read-string (slurp asset-file)))
                                                     (s/assert string?))
                                                "main.js")})]
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
