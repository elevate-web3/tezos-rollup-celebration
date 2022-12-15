(ns rollup.front.core
  (:require [clojure.core.async :as a :include-macros true]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [goog.dom :as d]
            [goog.string :as gstr]
            [orchestra.core :as _ :include-macros true]
            [rollup.front.util :as u]
            [wscljs.client :as ws]))

(s/def ::previous-count integer?)
(s/def ::previous-progress-percentage integer?)

(defn get-canvas-el! []
  (js/document.getElementById "my-canvas"))

(def event-ch
  (a/chan))

(def anim-frame-ch
  (a/chan))

(def ^:const transaction-completion 120000000)
(def ^:const bytes-per-message 6)

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
        C (case (bit-and b3 2r11)
            2r00 :R
            2r01 :G
            2r10 :B)
        V b4]
    #_{:row row :col col :account-number A :value V :color (case C
                                                           2r00 :R
                                                           2r01 :G
                                                           2r10 :B)}
    #js [row col A C V]))

(def ^:const node-width 100)
(def ^:const node-height 50)
(def ^:const canvas-width 2500)
(def ^:const canvas-height 2000)

(defn show-pixels [msg-vec]
  (let [el (get-canvas-el!)
        width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d"))
        image-data (.getImageData context 0 0 width height)
        data (.-data image-data)]
    (doseq [[row col account color value] msg-vec]
      (let [x-cell (rem account node-width)
            y-cell (quot account node-width)
            x (-> (* col node-width)
                  (+ x-cell))
            y (-> (* row node-height)
                  (+ y-cell))
            i (-> (* y canvas-width)
                  (+ x))]
        (aset data
              (+ (* i 4)
                 (case color
                   :R 0
                   :G 1
                   :B 2))
              value)))
    (-> context
        (.putImageData image-data 0 0))
    nil))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count ::previous-progress-percentage])]
  (fn animation-frame-handler-clsr [_]
    (a/put! anim-frame-ch m)))

(_/defn-spec start-update-loop nil?
  []
  (a/go-loop [msg-vec []]
    (let [[val port] (a/alts! [event-ch anim-frame-ch])]
      (cond
        (identical? port event-ch)
        (do #_(js/console.log (clj->js val))
            (recur (into msg-vec val)))
        ;; ---
        (identical? port anim-frame-ch)
        (let [{previous-count ::previous-count} val
              new-count (+ previous-count (count msg-vec))
              progress-percentage (let [percentage (-> (/ new-count transaction-completion)
                                                       (* 1000)
                                                       js/Math.floor
                                                       (/ 10))]
                                    (if (> percentage 100)
                                      100
                                      percentage))]
          (when-not (identical? new-count previous-count)
            ;; Update view
            #_(js/console.log (count msg-vec))
            (show-pixels msg-vec)
            (-> (d/getHTMLElement "transaction-count")
                (d/setTextContent (str new-count)))
            (when-not (identical? progress-percentage (::previous-progress-percentage val))
              (-> (js/document.getElementById "progress-bar")
                  (.setAttribute "style" (str "width:" progress-percentage "%;transition:opacity 0s linear;")))))
          (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count new-count
                                                                            ::previous-progress-percentage progress-percentage}))
          (recur [])))))
  ;; Start the UI refresh loop
  (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0
                                                                    ::previous-progress-percentage 0}))
  nil)

(defn start-system! []
  (ws/create
    (str "ws://" js/window.location.host "/data-stream")
    {:on-open (fn [_e]
                (-> (js/document.getElementById "loading-spinner") .-classList (.add "d-none"))
                (-> (js/document.getElementById "info-text") .-classList (.remove "d-none")))
     ;; :on-close (fn [e] (js/console.log "WS Close: " e))
     ;; :on-error (fn [e] (js/console.log "WS Error: " e))
     :on-message (fn [e]
                   (let [data (.-data e)]
                     ;; (js/console.log "Data: " data)
                     (if (string? data)
                       ;; String
                       (when-let [tps (some-> (edn/read-string data) :tps)]
                         ;; (js/console.log "String: " tps)
                         (-> (d/getHTMLElement "tps")
                             (d/setTextContent (str tps))))
                       ;; Blob
                       (-> (.arrayBuffer data)
                           (.then (fn [array-buffer]
                                    (let [uint-array (js/Uint8Array. array-buffer)
                                          ;; _ (js/console.log uint-array)
                                          quo (quot (.-length uint-array)
                                                    bytes-per-message)
                                          messages (for [i (range quo)]
                                                     (let [begin (* i bytes-per-message)
                                                           end (-> begin
                                                                   (+ bytes-per-message))]
                                                       (-> uint-array
                                                           (.slice begin end)
                                                           bytes->transaction)))]
                                      (a/put! event-ch (vec messages)))))))))})
  (u/reset-canvas (get-canvas-el!))
  (start-update-loop)
  nil)
