(ns rollup.server.util
  (:require [clojure.spec.alpha :as s]))

(s/def ::stream
  any?
  #_(instance? manifold.stream.default.Stream %))

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

(defn netty-buffer? [x]
  (instance? io.netty.buffer.UnpooledHeapByteBuf x))

(defn concat-byte-arrays [byte-arrays]
  (when (not-empty byte-arrays)
    (let [total-size (reduce + (map count byte-arrays))
          result     (byte-array total-size)
          bb         (java.nio.ByteBuffer/wrap result)]
      (doseq [ba byte-arrays]
        (.put bb ba))
      result)))

;; https://stackoverflow.com/questions/36019032/how-to-iterate-over-all-bits-of-a-byte-in-java
(defn byte->str [b]
  (-> (bit-and b 0xFF)
      Integer/toBinaryString
      (->> (format "%8s"))
      (.replace " " "0")))
