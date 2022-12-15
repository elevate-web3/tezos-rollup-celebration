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
        C (bit-and b3 2r11)
        V (bit-and b4 0xff)]
    {:row row
     :col col
     :account-number A
     :color (case C
              2r00 :R
              2r01 :G
              2r10 :B)
     :value V}))

(_/defn-spec create-animation-frame-handler fn?
  [m (s/keys :req [::previous-count ::previous-progress-percentage])]
  (fn animation-frame-handler-clsr [_]
    (a/put! anim-frame-ch m)))

(_/defn-spec start-update-loop nil?
  []
  (a/go-loop [byte-buffer-vec []]
    (let [[val port] (a/alts! [event-ch anim-frame-ch])]
      (cond
        (identical? port event-ch)
        (do #_(js/console.log (.-length val))
            (recur (conj byte-buffer-vec val)))
        ;; ---
        (identical? port anim-frame-ch)
        (let [{previous-count ::previous-count} val
              new-count (+ previous-count (count byte-buffer-vec))
              progress-percentage (let [percentage (-> (/ new-count transaction-completion)
                                                       (* 1000)
                                                       js/Math.floor
                                                       (/ 10))]
                                    (if (> percentage 100)
                                      100
                                      percentage))]
          (when-not (identical? new-count previous-count)
            ;; Update view
            #_(show-pixel-range previous-count byte-count)
            #_(-> (d/getHTMLElement "byte-count")
                  (d/setTextContent (-> byte-count
                                        str)))
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
                                    (js/console.log (clj->js
                                                      (bytes->transaction
                                                        (js/Uint8Array. array-buffer))))
                                    #_(->> (js/Uint8Array.from byte-buffer)
                                         (a/put! event-ch))
                                    #_(let [quo (quot (.-byteLength byte-buffer)
                                                    bytes-per-message)
                                          messages (for [i (range quo)]
                                                     (let [begin (* i bytes-per-message)
                                                           end (-> begin
                                                                   (+ (dec bytes-per-message)))]
                                                       (-> byte-buffer
                                                           (.slice begin end)
                                                           js/Uint8Array.from)))]
                                      (doseq [message messages]
                                        (a/put! event-ch message)))))))))})
  (start-update-loop)
  nil)

(comment
  (js/console.log
    (-> (js/ArrayBuffer. 12)
        .-byteLength))

  (let [[a b c] (js/Uint8Array. #js [1 2 3 4 5 6 7 8])]
    (js/console.log #js [a b c]))

  (let [ia (js/Uint8Array. #js [1 2 3 4 5 6 7 8])]
    (aget ia 0))

  (-> (range 12)
      js/ArrayBuffer.from
      (aget 1))

  (-> 6
      js/Math.floor)
  )
