(ns rollup.front.core
  (:require [clojure.core.async :as ca :include-macros true]
            [clojure.spec.alpha :as s]
            [goog.dom :as d]
            [orchestra.core :as _ :include-macros true]))

(s/def ::generate-events? boolean?)
(s/def ::number-of-events integer?)
(s/def ::previous-count integer?)
(s/def ::previous-progress-percentage integer?)
(s/def ::new-events integer?)
(s/def ::original-image-uint-array any?)
(s/def ::randomized-indexes (s/coll-of integer? :kind vector?))

(s/def ::state
  (s/keys :req [::generate-events? ::number-of-events]
          :opt [::original-image-uint-array ::randomized-indexes]))

(defn init! []
  (println "init"))

(defonce state*
  (atom {::generate-events? false
         ::number-of-events 0}))

(defn get-canvas-el! []
  (js/document.getElementById "my-canvas"))

(defn show-pixel-range [xi xe]
  (let [el (get-canvas-el!)
        width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (-> context
                       (.getImageData 0 0 width height))
        data (.-data image-data)
        original-data (::original-image-uint-array @state*)
        randomized-indexes (::randomized-indexes @state*)
        max-i (-> (.-length original-data)
                  (/ 4))]
    (when (< xi max-i)
      (loop [i xi]
        (when (and (<= i xe) (< i max-i))
          (let [rand-i (-> (get randomized-indexes i))]
            (doseq [i' [0 1 2 3]]
              (let [index (-> (* 4 rand-i) (+ i'))]
                (->> (aget original-data index)
                     (aset data index)))))
          (recur (inc i))))
      (-> context
          (.putImageData image-data 0 0)))
    nil))

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

(def event-ch
  (ca/chan))

(def anim-frame-ch
  (ca/chan))

(defn keydown-handler [e]
  (when (and (identical? (.-keyCode e) 32 ) ;; Spacebar
             (-> @state* ::generate-events? false?))
    (swap! state* assoc ::generate-events? true)
    (ca/go-loop []
      (when (-> @state* ::generate-events? true?)
        (-> (rand-int 200)
            inc
            (- 100)
            (+ 400)
            (as-> #_i <>
              (ca/put! event-ch {::new-events <>})))
        (ca/<! (ca/timeout 1))
        (recur)))))

(defn keyup-handler [e]
  (when (identical? (.-keyCode e) 32)
    (swap! state* assoc ::generate-events? false)))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count ::previous-progress-percentage])]
  (fn animation-frame-handler-clsr [_]
    (ca/put! anim-frame-ch m)))

(defn start-update-loop []
  (js/document.addEventListener "keydown" keydown-handler false)
  (js/document.addEventListener "keyup" keyup-handler false)
  (let [total-pixels (-> @state* ::original-image-uint-array .-length (/ 4))]
    (ca/go-loop [event-count 0]
      (let [[val port] (ca/alts! [event-ch anim-frame-ch])]
        (cond
          (identical? port event-ch)
          (recur (+ event-count (::new-events val)))
          ;; ---
          (identical? port anim-frame-ch)
          (let [previous-count (::previous-count val)
                progress-percentage (let [percentage (-> (/ event-count total-pixels)
                                                         (* 1000)
                                                         js/Math.floor
                                                         (/ 10))]
                                      (if (> percentage 100)
                                        100
                                        percentage))]
            (when-not (identical? event-count previous-count)
              ;; Update view
              (show-pixel-range previous-count event-count)
              (-> (d/getHTMLElement "event-count")
                  (d/setTextContent (str event-count)))
              (when-not (identical? progress-percentage (::previous-progress-percentage val))
                (-> (js/document.getElementById "progress-bar")
                    (.setAttribute "style" (str "width:" progress-percentage "%;transition:opacity 0s linear;")))))
            (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count event-count
                                                                              ::previous-progress-percentage progress-percentage}))
            (recur event-count))))))
  ;; Start the UI refresh loop
  (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0
                                                                    ::previous-progress-percentage 0})))

(defn start-system! []
  (let [img (js/Image.)
        canvas-el (get-canvas-el!)]
    (-> img
        (.addEventListener
          "load"
          (fn [_e]
            (-> canvas-el
                (.getContext "2d")
                (.drawImage img 0 0))
            (let [el (get-canvas-el!)
                  width (.-width el)
                  height (.-height el)
                  context (-> el (.getContext "2d"))
                  image-data (-> context
                                 (.getImageData 0 0 width height))
                  data (.-data image-data)
                  data-clone (js/Uint8ClampedArray.from data)]
              (swap! state* assoc
                     ::original-image-uint-array data-clone
                     ::randomized-indexes (-> (.-length data-clone)
                                              (/ 4)
                                              range
                                              shuffle
                                              vec))
              (start-update-loop)
              (reset-canvas canvas-el)
              (-> (js/document.getElementById "loading-spinner") .-classList (.add "d-none"))
              (-> (js/document.getElementById "info-text") .-classList (.remove "d-none"))))))
    #_(-> (.-crossOrigin img)
        (set! "anonymous"))
    (-> (.-src img)
        (set! "/images/tezos-rollup.jpg"))
    nil))

(defonce _system
  (start-system!))

;; ---
;; Exploratory dev code

(defn draw-original-data []
  (let [el (get-canvas-el!)
        width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (-> context
                       (.getImageData 0 0 width height))
        data (.-data image-data)
        original-data (get @state* ::original-image-uint-array)
        len (.-length original-data)]
    (loop [i 0]
      (when (< i len)
        (->> (aget original-data i)
             (aset data i))
        (recur (inc i))))
    (-> context
        (.putImageData image-data 0 0))
    nil))

(comment

  (draw-original-data)

  (set! js/window.foobar (get-canvas-el!))

  (do
    (-> (range 100000000)
        shuffle
        time)
    nil)

  (js/document.removeEventListener "keydown" keydown-handler)

  (ca/go-loop [i 0]
    (ca/<! (ca/timeout 500))
    (js/console.log i)
    (when (< i 10)
      (recur (inc i))))

  (def event-source
    (let [es (js/EventSource. "/data-stream")]
      (set! (.-onmessage es)
            (fn [event]
              (-> (d/getHTMLElement "event-count")
                  (d/setTextContent (str (.-data event))))))
      es))

  )
