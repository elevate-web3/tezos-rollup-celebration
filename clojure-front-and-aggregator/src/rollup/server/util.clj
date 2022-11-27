(ns rollup.server.util
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :as a]))

(s/def ::chan
  #(instance? clojure.core.async.impl.channels.ManyToManyChannel %))

;; An atom containing a stream or nil
(s/def ::stream<?>* #(instance? clojure.lang.Atom %))

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
