(ns rollup.front.util
  (:require [orchestra.core :as _ :include-macros true]
            [clojure.spec.alpha :as s]))

(s/def ::original-image-uint-array any?)
(s/def ::randomized-indexes (s/coll-of integer? :kind vector?))
(s/def ::canvas-el any?)

(defn reset-canvas [el]
  (let [width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (-> context
                       (.getImageData 0 0 width height))
        data (.-data image-data)
        len (.-length data)]
    (loop [i 0]
      (when (< i len)
        (aset data i 0)
        (aset data (+ i 1) 0)
        (aset data (+ i 2) 0)
        (aset data (+ i 3) 0)
        (recur (+ i 4))))
    (-> context
        (.putImageData image-data 0 0))
    nil))

(_/defn-spec wrap-show-pixel-range fn?
  [m (s/keys :req [::canvas-el ::original-image-uint-array ::randomized-indexes])]
  (let [{el ::canvas-el
         original-image-uint-array ::original-image-uint-array
         randomized-indexes ::randomized-indexes} m]
    (fn show-pixel-range [xi xe]
      (let [width (.-width el)
            height (.-height el)
            context (-> el (.getContext "2d"))
            image-data (-> context
                           (.getImageData 0 0 width height))
            data (.-data image-data)
            max-i (-> (.-length original-image-uint-array)
                      (/ 4))]
        (when (< xi max-i)
          (loop [i xi]
            (when (and (<= i xe) (< i max-i))
              (let [rand-i (-> (get randomized-indexes i))]
                (doseq [i' [0 1 2 3]]
                  (let [index (-> (* 4 rand-i) (+ i'))]
                    (->> (aget original-image-uint-array index)
                         (aset data index)))))
              (recur (inc i))))
          (-> context
              (.putImageData image-data 0 0)))
        nil))))

;; ---
;; Exploratory dev code

(defn draw-original-data [el img-uint-array]
  (let [width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (-> context
                       (.getImageData 0 0 width height))
        data (.-data image-data)
        original-data img-uint-array
        len (.-length original-data)]
    (loop [i 0]
      (when (< i len)
        (->> (aget original-data i)
             (aset data i))
        (recur (inc i))))
    (-> context
        (.putImageData image-data 0 0))
    nil))
