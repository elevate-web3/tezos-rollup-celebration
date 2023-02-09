(ns rollup.front.core
  (:require [clojure.core.async :as a :include-macros true]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [goog.dom :as d]
            [goog.style :as style]
            [orchestra.core :as _ :include-macros true]
            [rollup.front.util :as u]
            [rollup.shared.config :as sc]
            [rollup.shared.util :as su]
            [wscljs.client :as ws]))

(s/def ::previous-count integer?)
(s/def ::previous-progress-percentage integer?)

(s/def ::gauge any?) ;; The gauge JS instance object

(defn get-canvas-el! []
  (js/document.getElementById "my-canvas"))

(def event-ch
  (a/chan))

(def anim-frame-ch
  (a/chan))

(def tps-ch
  (a/chan))

(def ^:const transaction-completion 120000000)
(def ^:const bytes-per-message 6)

(def ^:const node-width 100)
(def ^:const node-height 50)
(def ^:const canvas-width 2500)
(def ^:const canvas-height 2000)

(defn interleave-commas [s]
  (->> s
       str/reverse
       (partition-all 3)
       (map #(apply str %))
       (str/join ",")
       str/reverse))

;; Check https://bernii.github.io/gauge.js/ for details
(defn create-gauge []
  (let [options {:angle 0.15, ;; The span of the gauge arc
                 :lineWidth 0.44, ;; The line thickness
                 :radiusScale 1, ;; Relative radius
                 :pointer {:length 0.6, ;; Relative to gauge radius
                           :strokeWidth 0.035, ;; The thickness
                           :color "#000000" ;; Fill color
                           },
                 :limitMax false,     ;; If false, max value increases automatically if value > maxValue
                 :limitMin false,     ;; If true, the min value of the gauge will be fixed
                 :colorStart "#6FADCF",   ;; Colors
                 :colorStop "#8FC0DA",    ;; just experiment with them
                 :strokeColor "#E0E0E0",  ;; to see which ones work best for you
                 :generateGradient true,
                 :highDpiSupport true,     ;; High resolution support
                 }
        target (js/document.getElementById "gauge-canvas")
        gauge (-> (js/Gauge. target)
                  (.setOptions options))]
    (set! (-> gauge .-maxValue) sc/gauge-max-value)
    (-> gauge (.setMinValue sc/gauge-min-value))
    (set! (-> gauge .-animationSpeed) 32)
    (-> gauge (.set sc/gauge-min-value))
    gauge))

(defn make-show-pixels-fn []
  (let [el (get-canvas-el!)
        width (.-width el)
        height (.-height el)
        context (-> el (.getContext "2d" #js {"willReadFrequently" true}))]
    (fn show-pixels [msg-vec]
      (let [image-data (.getImageData context 0 0 width height)
            data (.-data image-data)]
        (doseq [[row col account color value #_:as #_msg] msg-vec]
          ;; (js/console.log msg)
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
        nil))))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count ::previous-progress-percentage])]
  (fn animation-frame-handler-clsr [_]
    (a/put! anim-frame-ch m)))

(_/defn-spec start-update-loop nil?
  [m (s/keys :req [::gauge])]
  (let [{gauge ::gauge} m
        show-pixels (make-show-pixels-fn)
        init-time (js/Date.now)]
    (a/go-loop [msg-vec []
                tps-count 0]
      (let [[val port] (a/alts! [event-ch anim-frame-ch tps-ch])]
        (cond
          ;; Accumulate message
          (identical? port event-ch)
          (recur (into msg-vec val)
                 (+ tps-count (count val)))
          ;; Update TPS
          (identical? port tps-ch)
          (if (> tps-count 0)
            ;; Update mean TPS
            (let [now (js/Date.now)
                  mean-tps (-> (- now init-time)
                               (/ 1000)
                               (->> (/ tps-count)))]
              (-> (d/getHTMLElement "tps")
                  (d/setTextContent (-> mean-tps
                                        js/Math.round
                                        str
                                        interleave-commas)))
              (.set gauge (let [tps (js/Math.round mean-tps)]
                            (if (> tps sc/gauge-max-value)
                              sc/gauge-max-value
                              tps)))
              (recur msg-vec tps-count))
            ;; Wait for incoming messages
            (recur msg-vec tps-count))
          ;; Rerender the image and update the progress bar
          (identical? port anim-frame-ch)
          (let [{previous-count ::previous-count} val
                new-count (+ previous-count (count msg-vec))
                progress-percentage (let [percentage (-> (/ new-count transaction-completion)
                                                         (* 100000)
                                                         js/Math.floor
                                                         (/ 1000))]
                                      (if (> percentage 100)
                                        100
                                        percentage))]
            (when-not (identical? new-count previous-count)
              ;; Update view
              (show-pixels msg-vec)
              (-> (d/getHTMLElement "transaction-count")
                  (d/setTextContent (-> new-count
                                        (/ 100000)
                                        (js/Math.round)
                                        (/ 10))))
              (when-not (identical? progress-percentage (::previous-progress-percentage val))
                (-> (js/document.getElementById "progress-bar")
                    (style/setStyle "height" (str progress-percentage "%")))))
            (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count new-count
                                                                              ::previous-progress-percentage progress-percentage}))
            (recur [] tps-count))))))
  (js/setInterval
    (fn [] (a/put! tps-ch true))
    sc/gauge-refresh-interval-ms)
  ;; Start the UI refresh loop
  (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0
                                                                    ::previous-progress-percentage 0}))
  nil)

(defn start-system! []
  (let [gauge (create-gauge)]
    (ws/create
      (str "ws://" js/window.location.host "/data-stream")
      {:on-open (fn [_e]
                  (start-update-loop {::gauge gauge}))
       ;; :on-close (fn [e] (js/console.log "WS Close: " e))
       ;; :on-error (fn [e] (js/console.log "WS Error: " e))
       :on-message (fn [e]
                     (let [data (.-data e)]
                       ;; (js/console.log "Data: " data)
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
                                                           su/bytes->transaction)))]
                                      (a/put! event-ch (vec messages))))))))}))
  (u/reset-canvas (get-canvas-el!))
  nil)
