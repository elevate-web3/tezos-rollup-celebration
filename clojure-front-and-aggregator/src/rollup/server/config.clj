(ns rollup.server.config
  (:require [clojure.spec.alpha :as s]))

;; Time in ms
(s/def ::ticking-time integer?)
