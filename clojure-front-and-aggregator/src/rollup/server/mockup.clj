(ns rollup.server.mockup
  (:require [clojure.spec.alpha :as s]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.util :as u]))

(s/def ::interval integer?)
(s/def ::msg-size integer?)

(defn make-random-message []
  ;; 010AAAAA 100AAAAA 110AAACC VVVVVVVV
  (let [b1 (bit-and (rand-int 256) 2r01011111)
        b2 (bit-and (rand-int 256) 2r10011111)
        b3 (let [color (case (rand-int 3)
                         0 2r00
                         1 2r01
                         2 2r10)
                 vvv (-> (rand-int 256)
                         (bit-and 2r00000111))]
             (-> (bit-shift-left 2r110 3)
                 (bit-and vvv)
                 (bit-shift-left 2)
                 (bit-and color)))
        b4 (rand-int 256)]
    (byte-array [b1 b2 b3 b4])))

(_/defn-spec get-static-mockup-stream any?
  [m (s/keys :req [::msg-size ::interval])]
  (let [rand-bytes (u/concat-byte-arrays
                     (for [_i (range (::msg-size m))]
                       (make-random-message)))]
    (ms/periodically
      (::interval m)
      (fn [] (u/concat-byte-arrays [rand-bytes])))))

(_/defn-spec get-random-mockup-stream any?
  [m (s/keys :req [::msg-size ::interval])]
  (ms/periodically
    (::interval m)
    (fn []
      (-> (for [_i (range (::msg-size m))]
            (make-random-message))
          u/concat-byte-arrays))))
