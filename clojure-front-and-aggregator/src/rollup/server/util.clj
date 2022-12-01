(ns rollup.server.util
  (:require [clojure.spec.alpha :as s]))

(s/def ::stream
  #(instance? manifold.stream.default.Stream %))

;; An atom containing a stream or nil
(s/def ::stream<?>* #(and (instance? clojure.lang.Atom %)
                          (let [val (deref %)]
                            (or (nil? val)
                                (instance? manifold.stream.default.Stream val)))))

;; Handler meant to stop light processes
(s/def ::clean-fn fn?)

(let [c (class (byte-array 1))]
  (defn byte-array? [x]
    (instance? c x)))
