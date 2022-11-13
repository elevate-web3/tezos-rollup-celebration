(ns tezos-rollup.core
  (:require [clojure.core.async :as ca :include-macros true]
            [clojure.spec.alpha :as s]
            [goog.dom :as d]
            [orchestra.core :as _ :include-macros true]))

(s/def ::generate-events? boolean?)
(s/def ::number-of-events integer?)
(s/def ::previous-count integer?)
(s/def ::new-events integer?)

(defn init! []
  (println "init"))

(defonce state*
  (atom {::generate-events? false
         ::number-of-events 0}))

(defn get-canvas-el! []
  (js/document.getElementById "my-canvas"))

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

(comment
  (-> (get-canvas-el!) reset-canvas)
  )

(defn increase-alpha-30 [el]
  (let [width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (-> context (.getImageData 0 0 width height))
        data (.-data image-data)]
    (doseq [y (range height)
            x (range width)]
      (let [pos (-> (* y width) (+ x))
            i (* pos 4)]
        ;; (aset data i 0)
        ;; (aset data (+ i 1) 0)
        ;; (aset data (+ i 2) 0)
        (->> (aget data (+ i 3))
             (+ 30)
             (aset data (+ i 3)))))
    (-> context
        (.putImageData image-data 0 0))
    nil))

(comment
  (-> (get-canvas-el!) increase-alpha-30)
  )

(def event-ch
  (ca/chan))

(def anim-frame-ch
  (ca/chan))

(defn keydown-handler [_]
  (when (-> @state* ::generate-events? false?)
    (swap! state* assoc ::generate-events? true)
    (ca/go-loop []
      (when (-> @state* ::generate-events? true?)
        (-> (rand-int 200)
            inc
            (- 100)
            (+ 1000)
            (as-> #_i <>
              (ca/put! event-ch {::new-events <>})))
        (ca/<! (ca/timeout 1))
        (recur)))))

(defn keyup-handler [_]
  (swap! state* assoc ::generate-events? false))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count])]
  (fn animation-frame-handler-clsr [_]
    (ca/put! anim-frame-ch m)))

(comment
  ;; Eval following block to start the update loop
  (do (js/document.addEventListener "keydown" keydown-handler false)
      (js/document.addEventListener "keyup" keyup-handler false)
      (ca/go-loop [event-count 0]
        (let [[val port] (ca/alts! [event-ch anim-frame-ch])]
          (cond
            (identical? port event-ch)
            (recur (+ event-count (::new-events val)))
            ;; ---
            (identical? port anim-frame-ch)
            (do (when-not (identical? event-count (::previous-count val))
                  ;; Update view
                  (-> (d/getHTMLElement "event-count")
                      (d/setTextContent (str event-count))))
                (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count event-count}))
                (recur event-count)))))
      ;; Start the UI refresh loop
      (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0})))
  )

;; ---
;; Exploratory dev code

(comment

  (let [img (js/Image.)
        canvas-el (get-canvas-el!)]
    (-> (.-src img)
        (set! "https://avatars.githubusercontent.com/u/450766?v=4"))
    (-> canvas-el
        (.getContext "2d")
        (.drawImage img 0 0))
    )

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

  )
