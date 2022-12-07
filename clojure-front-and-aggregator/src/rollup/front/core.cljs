(ns rollup.front.core
  (:require [clojure.core.async :as a :include-macros true]
            [clojure.spec.alpha :as s]
            [goog.dom :as d]
            [goog.string :as gstr]
            [rollup.front.util :as u]
            [orchestra.core :as _ :include-macros true]))

(s/def ::previous-count integer?)
(s/def ::previous-progress-percentage integer?)
(s/def ::new-events integer?)

(defn get-canvas-el! []
  (js/document.getElementById "my-canvas"))

(def event-ch
  (a/chan))

(def anim-frame-ch
  (a/chan))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count ::previous-progress-percentage])]
  (fn animation-frame-handler-clsr [_]
    (a/put! anim-frame-ch m)))

(_/defn-spec start-update-loop nil?
  [m (s/keys :req [::u/canvas-el ::u/original-image-uint-array ::u/randomized-indexes])]
  (let [total-pixels (-> m ::u/original-image-uint-array .-length (/ 4))
        show-pixel-range (u/wrap-show-pixel-range m)]
    (a/go-loop [byte-count 0]
      (let [[val port] (a/alts! [event-ch anim-frame-ch])]
        (cond
          (identical? port event-ch)
          (recur (-> (count val)
                     (/ 2) ;; For the time being bytes are sent as hex string. One byte is two characters.
                     (+ byte-count)))
          ;; ---
          (identical? port anim-frame-ch)
          (let [previous-count (::previous-count val)
                progress-percentage (let [percentage (-> (/ byte-count total-pixels)
                                                         (* 1000)
                                                         js/Math.floor
                                                         (/ 10))]
                                      (if (> percentage 100)
                                        100
                                        percentage))]
            (when-not (identical? byte-count previous-count)
              ;; Update view
              (show-pixel-range previous-count byte-count)
              (-> (d/getHTMLElement "byte-count")
                  (d/setTextContent (str byte-count)))
              (when-not (identical? progress-percentage (::previous-progress-percentage val))
                (-> (js/document.getElementById "progress-bar")
                    (.setAttribute "style" (str "width:" progress-percentage "%;transition:opacity 0s linear;")))))
            (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count byte-count
                                                                              ::previous-progress-percentage progress-percentage}))
            (recur byte-count))))))
  ;; Start the UI refresh loop
  (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0
                                                                    ::previous-progress-percentage 0}))
  nil)

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
              (let [es (js/EventSource. "/data-stream")]
                (.addEventListener es "bytes" (fn [event]
                                                (a/put! event-ch (.-data event))))
                (.addEventListener es "tps" (fn [event]
                                              (-> (d/getHTMLElement "tps")
                                                  (d/setTextContent (.-data event))))))
              (start-update-loop {::u/canvas-el el
                                  ::u/original-image-uint-array data-clone
                                  ::u/randomized-indexes (-> (.-length data-clone)
                                                             (/ 4)
                                                             range
                                                             shuffle
                                                             vec)})
              (u/reset-canvas canvas-el)
              (-> (js/document.getElementById "loading-spinner") .-classList (.add "d-none"))
              (-> (js/document.getElementById "info-text") .-classList (.remove "d-none"))))))
    #_(-> (.-crossOrigin img)
        (set! "anonymous"))
    (-> (.-src img)
        (set! "/images/tezos-rollup.jpg"))
    nil))
