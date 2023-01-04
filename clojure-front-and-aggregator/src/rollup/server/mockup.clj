(ns rollup.server.mockup
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [rollup.server.util :as u])
  (:import io.netty.buffer.Unpooled))

(s/def ::interval integer?)
(s/def ::msg-size integer?)
(s/def ::row integer?)
(s/def ::column integer?)

(defn make-random-message []
  ;; 010AAAAA 100AAAAA 110AAACC VVVVVVVV
  (let [account (rand-int 5000)
        bit5 2r11111
        bit3 2r111
        part3 (bit-and account bit3)
        part2 (-> (bit-shift-right account 3)
                  (bit-and bit5))
        part1 (-> (bit-shift-right account 8)
                  (bit-and bit5))
        b1 (bit-or part1 2r01000000)
        b2 (bit-or part2 2r10000000)
        b3 (let [color (case (rand-int 3)
                         0 2r00
                         1 2r01
                         2 2r10)]
             (-> (bit-shift-left part3 2)
                 (bit-or color)
                 (bit-or 2r11000000)))
        b4 (rand-int 256)]
    ;; (println (->> (map u/byte->str [b1 b2 b3 b4])
    ;;               (str/join " ")))
    (byte-array [b1 b2 b3 b4])))

(_/defn-spec get-static-mockup-stream any?
  [m (s/keys :req [::msg-size ::interval ::row ::column])]
  (let [rand-bytes (u/concat-byte-arrays
                     (for [_i (range (::msg-size m))]
                       (u/concat-byte-arrays [(byte-array [(::row m) (::column m)])
                                              (make-random-message)])))]
    (ms/periodically
      (::interval m)
      (fn [] (Unpooled/wrappedBuffer
               (u/concat-byte-arrays [rand-bytes]))))))

(_/defn-spec get-random-mockup-stream any?
  [m (s/keys :req [::msg-size ::interval ::row ::column])]
  (ms/periodically
    (::interval m)
    (fn []
      (Unpooled/wrappedBuffer
        (u/concat-byte-arrays
          (for [_i (range (::msg-size m))]
            (u/concat-byte-arrays [(byte-array [(::row m) (::column m)])
                                   (make-random-message)])))))))
