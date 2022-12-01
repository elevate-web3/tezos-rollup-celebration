(ns rollup.server.util
  (:require [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [manifold.stream :as ms]))

(s/def ::chan
  #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))

(s/def ::stream
  #(instance? manifold.stream.default.Stream %))

;; An atom containing a stream or nil
(s/def ::stream<?>* #(and (instance? clojure.lang.Atom %)
                          (let [val (deref %)]
                            (or (nil? val)
                                (instance? manifold.stream.default.Stream val)))))

;; Handler meant to stop light processes
(s/def ::clean-fn fn?)

(defn throw-on-err [v]
  (if (isa? java.lang.Throwable v)
    (throw v)
    v))

(defmacro <? "Version of <! that throw Exceptions that come out of a channel."
  [c]
  `(let [v# (a/<! ~c)]
     (if (isa? java.lang.Throwable v#)
       (throw v#)
       v#)))
