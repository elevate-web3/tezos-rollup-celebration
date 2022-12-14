(ns rollup.server.handlers.public
  (:require [aleph.http :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [hiccup.util :as hu]
            [hiccup2.core :as h]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.core :as _]
            [ring.util.response :as resp]
            [rollup.server.aggregator :as aggregator]
            [rollup.server.routing :as rtng]))

(defn layout [body]
  (str
    (h/html
      {:mode :html}
      (hu/raw-string "<!DOCTYPE html>\n")
      body)))

(let [background-color "#B9C2CF"
      main-js-name (if-let [asset-file (io/resource "public/cljs/assets.edn")]
                     (->> (some #(when (-> % :name (= :main))
                                   (:output-name %))
                                (edn/read-string (slurp asset-file)))
                          (s/assert string?))
                     "main.js")]
  (defn home-page [_req]
    (-> (layout
          [:html {:class "h-100"
                  :style {:background-color background-color}}
           [:head
            [:meta {:charset "utf-8"}]
            [:link {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css",
                    :rel "stylesheet",
                    :integrity "sha384-Zenh87qX5JnK2Jl0vWa8Ck2rdkQ2Bzep5IDxbcnCeuOxjzrPF/et3URy9Bv1WTRi",
                    :crossorigin "anonymous"}]
            [:title "Tezos Rollup celebration"]
            [:style "--bs-progress-bar-transition: opacity 0s linear;"]]
           [:body {:style {:background-color background-color}}
            [:div.d-flex.justify-content-center.align-items-center.position-relative {:style {:height "100px"}}
             [:img.position-absolute {:style {:width "200px"
                                              :top "10px"
                                              :left "10px"}
                                      :src "/images/TezosLogo_Horizontal_Black.svg"}]
             [:div.h1
              "Race to 120 million"]]
            [:canvas#my-canvas.d-block.mx-auto.mt-3
             {:style "border: 1px solid black;", :width "2500", :height "2000"}]
            [:div.text-center.mt-4
             [:div.d-inline-block
              {:style "width: 680px;"}
              [:div.progress
               [:div#progress-bar.progress-bar {:style "width: 0%;transition:opacity 0s linear;",
                                                :role "progressbar",
                                                :aria-label "Basic example",
                                                :aria-valuenow "0",
                                                :aria-valuemin "0",
                                                :aria-valuemax "100"}]]]]
            [:div.mt-3.text-center
             [:span.h3 "Bytes received: "]
             [:span#byte-count.h3 "0"]]
            [:div.mt-3.text-center
             [:span.h3 "TPS: "]
             [:span#tps.h3 "0"]]
            [:h5#info-text.mt-3.text-center.d-none "Client ready for incoming data"]
            [:div#loading-spinner.mt-3.text-center
             [:div.spinner-border {:role "status"} [:span.visually-hidden "Loading..."]]
             [:h5.mt-2 "Loading..."]]
            [:script {:src (str "/cljs/" main-js-name)}]]])
        resp/response
        (resp/content-type "text/html; charset=utf-8"))))

;; https://gist.github.com/jeroenvandijk/67d064e0bb08b900e656
(_/defn-spec data-stream ::rtng/response
  [req ::rtng/request]
  (let [{source-stream ::aggregator/output-stream
         websockets* ::rtng/websockets*} req
        stream (ms/stream)]
    (-> (md/let-flow [socket (http/websocket-connection req)]
          (swap! websockets* conj socket)
          (ms/on-closed stream (fn clean []
                                 (swap! websockets* disj socket)))
          (ms/connect source-stream socket)
          socket)
        (md/catch (fn [_]
                    {:status 400
                     :headers {"content-type" "application/text"}
                     :body "Expected a websocket request."})))))
