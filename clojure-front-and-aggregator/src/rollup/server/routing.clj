(ns rollup.server.routing
  (:require [clojure.spec.alpha :as s]
            [reitit.core :as reitit]
            [rollup.server.collector :as collector])
  (:import clojure.lang.Atom))

(s/def ::websockets* #(and (instance? Atom %)
                           (-> % deref set?)))
(s/def ::request (s/keys :req [::collector/output-stream
                               ::reitit/match
                               ::reitit/router
                               ::websockets*]))

(s/def ::response (s/keys :req-un [::headers ::status ::body]))

(s/def ::name keyword?)
(s/def ::path string?)
(s/def ::path-params (s/map-of keyword? string?))
(s/def ::query-params (s/map-of string? string?))

;; ---

;; (s/fdef name->path
;;   :args (s/cat :m (s/keys :req [::req ::name]
;;                           :opt [::path-params ::query-params]))
;;   :ret string?)

;; (defn name->path [m]
;;   (-> (::req m)
;;       ::reitit/router
;;       (reitit/match-by-name (::name m) (::path-params m))
;;       (reitit/match->path (::query-params m))))
