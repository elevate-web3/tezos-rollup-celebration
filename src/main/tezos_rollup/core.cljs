(ns tezos-rollup.core
  (:require [clojure.core.async :as ca :include-macros true]))

(defn init! []
  (println "init"))

(defonce state*
  (atom {:generate-events? false}))

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

(defn key-press-handler [_]
  (js/console.log "keyown"))

(defn create-animation-frame-handler [previous-count]
  (fn animation-frame-handler-clsr [_]
    (ca/put! anim-frame-ch {:previous-count previous-count})))

(comment

  (ca/go-loop [event-count 0]
    (let [[val port] (ca/alts! [event-ch anim-frame-ch])]
      (cond
        (identical? port event-ch)
        (recur (+ event-count (:new-events val)))
        ;; ---
        (identical? port anim-frame-ch)
        (do (if (identical? event-count (:previous-count val))
              (js/console.log "No count change")
              (js/console.log "New count"))
            (js/window.requestAnimationFrame (create-animation-frame-handler {:previous-count event-count}))
            (recur event-count)))))

  (js/window.requestAnimationFrame (create-animation-frame-handler {:previous-count 0}))

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

  (js/document.addEventListener "keydown" key-press-handler false)

  (js/document.removeEventListener "keydown" key-press-handler)

  (ca/go-loop [i 0]
    (ca/<! (ca/timeout 500))
    (js/console.log i)
    (when (< i 10)
      (recur (inc i))))

  )
