(ns rollup.server.handlers.public
  (:require [ring.util.response :as resp]
            [clojure.java.io :as io]))

(defn home-page [_req]
  (-> (io/resource "public/index.html")
      slurp
      resp/response
      (resp/content-type "text/html; charset=utf-8")))

(defn data-stream [req]
  ;; WIP
  )
