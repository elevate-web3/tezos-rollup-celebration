(ns rollup.shared.util)

(defn bytes->transaction [uint-array]
  (let [row (aget uint-array 0)
        col (aget uint-array 1)
        b1 (aget uint-array 2)
        b2 (aget uint-array 3)
        b3 (aget uint-array 4)
        b4 (aget uint-array 5)
        ;; ---
        A1 (bit-and b1 2r11111111)
        A2 (bit-and b2 2r11111)
        A (-> A2
              (* 0x100)
              (+ A1))
        C (case b3
            82 :R
            71 :G
            66 :B)
        V (bit-and b4 0xff)]
    #?(:cljs #js [row col A C V]
       :clj [row col A C V])))
