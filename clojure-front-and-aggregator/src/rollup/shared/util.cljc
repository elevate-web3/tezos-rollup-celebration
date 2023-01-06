(ns rollup.shared.util)

(defn bytes->transaction [uint-array]
  (let [row (aget uint-array 0)
        col (aget uint-array 1)
        b1 (aget uint-array 2)
        b2 (aget uint-array 3)
        b3 (aget uint-array 4)
        b4 (aget uint-array 5)
        ;; ---
        A1 (bit-and b1 2r11111)
        A2 (bit-and b2 2r11111)
        A3 (-> (bit-and b3 2r11100)
               (bit-shift-right 2))
        A (-> A1
              (* 2r100000)
              (+ A2)
              (* 2r1000)
              (+ A3))
        C (let [color (bit-and b3 2r11)]
            (case color
              2r00 :R
              2r01 :G
              2r10 :B
              :R))
        V (bit-and b4 0xff)]
    #?(:cljs (do #js [row col A C V])
       :clj [row col A C V])))
