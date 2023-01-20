(ns rollup.front.core
  (:require [clojure.core.async :as a :include-macros true]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [goog.dom :as d]
            [goog.style :as style]
            [orchestra.core :as _ :include-macros true]
            [rollup.front.util :as u]
            [rollup.shared.util :as su]
            [wscljs.client :as ws]))

(s/def ::previous-count integer?)
(s/def ::previous-progress-percentage integer?)

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
  []
  (let [show-pixels (make-show-pixels-fn)]
    (a/go-loop [msg-vec []
                tps-count 0]
      (let [[val port] (a/alts! [event-ch anim-frame-ch tps-ch])]
        (cond
          (identical? port event-ch)
          (do #_(js/console.log (clj->js val))
              (recur (into msg-vec val)
                     (+ tps-count (count val))))
          ;; ---
          (identical? port tps-ch)
          (do (some-> (d/getHTMLElement "tps")
                      (d/setTextContent (str tps-count)))
              (recur msg-vec 0))
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
              (some-> (d/getHTMLElement "transaction-count")
                      (d/setTextContent (str new-count)))
              (when-not (identical? progress-percentage (::previous-progress-percentage val))
                (some-> (js/document.getElementById "progress-bar")
                        (style/setStyle "width" (str progress-percentage "%")))))
            (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count new-count
                                                                              ::previous-progress-percentage progress-percentage}))
            (recur []
                   tps-count))))))
  (js/setInterval
    (fn [] (a/put! tps-ch true))
    1000)
  ;; Start the UI refresh loop
  (js/window.requestAnimationFrame (create-animation-frame-handler {::previous-count 0
                                                                    ::previous-progress-percentage 0}))
  nil)

(defn start-system! []
  (ws/create
    (str "ws://" js/window.location.host "/data-stream")
    {:on-open (fn [_e]
                (some-> (js/document.getElementById "loading-spinner") .-classList (.add "d-none"))
                (some-> (js/document.getElementById "info-text") .-classList (.remove "d-none")))
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
                                    (a/put! event-ch (vec messages))))))))})
  (u/reset-canvas (get-canvas-el!))
  (start-update-loop)
  nil)
