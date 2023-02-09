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
            [rollup.server.collector :as collector]
            [rollup.server.config :as c]
            [rollup.server.routing :as rtng]
            [rollup.shared.config :as sc]
            [rollup.shared.util :as su]))

(def ^:const tezos-blue
  "#0F61FF")

(def ^:const site-background
  "linear-gradient(90deg, rgba(15,97,255,1) 0%, rgba(15,97,255,1) 72%, rgba(159,50,159,1) 100%)")

(def ^:const tezos-grey-stealth
  "#1D2227")

(def ^:const tezos-grey-light
  "#AEB1B9")

(def ^:const tezos-grey-dark
  "#030405")

(def ^:const tezos-light-dark
  "#263042")

(defn layout [body]
  (-> (h/html
        {:mode :html}
        (hu/raw-string "<!DOCTYPE html>\n")
        body)
      str
      resp/response
      (resp/content-type "text/html; charset=utf-8")))

(let [main-js-name (if-let [asset-file (io/resource "public/cljs/assets.edn")]
                     (->> (some #(when (-> % :name (= :main))
                                   (:output-name %))
                                (edn/read-string (slurp asset-file)))
                          (s/assert string?))
                     "main.js")]
  (defn home-page [_req]
    (layout
      [:html {:class "h-100"
              :style {:background site-background}}
       [:head
        [:meta {:charset "utf-8"}]
        [:link {:href "https://cdn.jsdelivr.net/npm/bootstrap@5.2.2/dist/css/bootstrap.min.css",
                :rel "stylesheet",
                :integrity "sha384-Zenh87qX5JnK2Jl0vWa8Ck2rdkQ2Bzep5IDxbcnCeuOxjzrPF/et3URy9Bv1WTRi",
                :crossorigin "anonymous"}]
        [:link {:rel "preconnect", :href "https://fonts.googleapis.com"}]
        [:link {:rel "preconnect", :href "https://fonts.gstatic.com"}]
        [:link
         {:href "https://fonts.googleapis.com/css2?family=Poppins&display=swap",
          :rel "stylesheet"}]
        [:title "Tezos Rollup celebration"]
        [:style "--bs-progress-bar-transition: opacity 0s linear;"]
        [:style (str ".form-check-input:checked{background-color:" tezos-light-dark ";border-color:" tezos-light-dark ";}")]]
       [:body {:style {:background site-background
                       :color "white"
                       :font-family "Poppins"}}
        [:nav.navbar.navbar-dark.navbar-expand.position-fixed.w-100 {:style {:background-color tezos-grey-dark
                                                                             :z-index 1000}}
         [:div.row.w-100
          [:div.col-3
           [:a.navbar-brand {:href sc/tezos-logo-url}
            [:img {:style {:height "50px"}
                   :src "/images/TezosLogo_Horizontal_White.svg"}]]]
          [:div.col-6.d-flex.justify-content-center.align-items-center
           [:h2.mb-0 "Race to 120 million transactions"]]
          [:div.col-3.d-flex.justify-content-end.align-items-center
           [:ul.navbar-nav.position
            [:li.nav-item.fw-bold.pe-3
             [:a.nav-link {:href sc/blog-post-url
                           :style {:color "white"
                                   :font-size "1.25rem"}}
              sc/blog-post-text]]]]]]
        [:div {:style {:height "76px"}}]
        ;; Data section
        [:div.container-fluid.text-center
         [:div.row
          [:div.col-2.py-4.d-flex
           [:div.flex-fill.d-flex.flex-column.justify-content-center.align-items-center
            [:p
             [:span#transaction-count.h4 "0"]
             [:span.h4 "M"]
             [:span.h4 "/120M"]]
            [:div.flex-fill.d-flex.flex-column.justify-content-center #_{:style {:height "calc(100vh - 10px)"}}
             [:div.progress {:style {:background-color tezos-grey-light
                                     :transform "rotate(180deg)"
                                     :width "15px"
                                     :height "calc(100vh - 190px)"}}
              [:div#progress-bar.progress-bar {:style {:width "100%"
                                                       :transition "opacity 0s linear"
                                                       :background-color tezos-grey-stealth}
                                               :role "progressbar",
                                               :aria-label "Basic example",
                                               :aria-valuenow "0",
                                               :aria-valuemin "0",
                                               :aria-valuemax "100"}]]]]]
          [:div.col-8.pt-2
           [:canvas#my-canvas.d-block.m-auto {:width "2500"
                                              :height "2000"
                                              :style {:border "1px solid black"
                                                      :height sc/canvas-height-prop}}]]
          [:div.col-2.d-flex.flex-column.justify-content-center.align-items-center
           [:div#tps.h3 0]
           [:canvas#gauge-canvas]
           [:p "Mean TPS"]]]]
        ;; Explanation section
        [:div.container-fluid.mt-5
         [:h1 "An explanation on what you see"]
         [:div.row.row-cols-1.row-cols-sm-2.gy-3.mt-3
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:h3 "1000 transactions per second"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:h3 "Why the next generation of optimistic rollups are a game changer on Tezos"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]]]
        ;; Teams section
        [:div.container-fluid.mt-5
         [:h1 "Teams"]
         [:div.row.row-cols-1.row-cols-sm-3.gy-3.mt-3
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:div.text-center.mb-2
              [:img {:style {:width "300px"}
                     :src "https://i0.wp.com/infrachain.com/wp-content/uploads/2022/02/Nomadic-Labs-Logo-768x156-2.png?w=768&ssl=1"}]]
             [:h3 "Nomadic Labs"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.text-center
             [:img {:style {:width "100px"}
                    :src "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAALgAAAC4CAMAAABn7db1AAAAV1BMVEX///8AAAABAQGAgIBAQED8/Pw/Pz/5+fn39/cGBgYJCQk8PDw5OTnz8/NDQ0N8fHyEhIQQEBDt7e2Xl5e3t7eKiop1dXVubm5JSUmhoaFPT08bGxusrKwxmMZ5AAADaElEQVR4nO2c63LaMBBGJcHq4ivQJqGX93/O7grazqRIHmsbMZP5zpBfjqzDshZYK8sYAAAAAAAAAAAAAAAAAAD8QzBfDutBXn35yj0bQ9RuflkGm1KynXkJZIJC25jZO+usuHflNcqnbdrVKXjrkht6R/xgsnVQhPyYA+46cyLiTFHkSqQxDYmzpTOrXJcUFQE3E0t3vzZzqqjEA3m2foY4BaMbV3hUkSzvLm40YwrEIQ5xiEMc4hCHOMQh/knEJz7N0N18JeW9cjQ+PeGW053irIw4TY4Dfu4dcUkVmRNq956ns+0/kWVf2TloZlWC8YPcLHdPFTOTynzmi/OcXPeL8zX3HhVZPl/zTFZv8ZcLZ2m7taRKnpTRjagNEPd777v1BEbs94vzOEwt7f62V03Wcqrceqe95KDR/nZ3wqyb1per08SGmTBudsmfdis8oqhCHu8ljf0Bj3nmrzXiF5NPohgP6W1c/HXZi5/euNs4725459st7O3ewSx2aBgNkx1zt63D4ZqtleLJDrtLKc4t0m1sFT/wx9VuLcRRvu53d+yszxdYq/gq14fmt0rzjYQ75l/UreLPuwOCOMQhDnGIQxziEIc4xCEOcYhDHOIQ/1/io30ongZZ5Vye4doQd1IRq8RjlUl9zWyviUuSEtD74lKuaNUm5jbF+W8oF8VWCtr142/TNB2vx3csk/+eagviN1NF2v54f9o//DSaIqd4mxgMPZyAnH1tEndDPC/YPR+KMb1kbdVs7eP2kd/KWFsQv31xDsmt5WTIK5k1MZfCwINSjhRoptpgsxXxJI1P5YpFLgGpkvxxuVBO6lNqjziPSsmVUyX87qYVKSE9WvPPiU9LJeCbEeeAD7YszqmiKTZWoPz0RDlZNnNcvgYOH2EGcYhDHOIQhzjEIQ5xiEMc4hCHOMQhDnGIQxziEIc4xCEOcYhDHOIQhzjEIQ7xzyge8gYmRa2Un6ANVPkHfp36e8tj8152jClg7TFv4Fo8npJLz4h4MDS/+tEXFw5e1/xYfemwH6dpuX5TPpfcIk6xvhKT5rwRQvHwbXuJZxACUXkDlPuBohq/pzl80HKxKjPddoYoLhukGOVtlY6He+vu4rnb2gPo2aySTCHSc7TDRoqyVu24vCXlPiQAAAAAAAAAAAAAoIFfRuxUUNXoC2EAAAAASUVORK5CYII="}]]
            [:div.card-body
             [:h3 "TriliTech"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
[:div.text-center
             [:img {:style {:width "125px"}
                    :src "/images/logo-elevate_v01.png"}]]
             [:h3 "Elevate"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]]]
        ;; Footer space
        [:div {:style {:height "50px"}}]
        [:script {:src "/js/gauge.js"}]
        [:script {:src (str "/cljs/" main-js-name)}]]])))

;; https://gist.github.com/jeroenvandijk/67d064e0bb08b900e656
(_/defn-spec data-stream ::rtng/response
  [req ::rtng/request]
  (let [{source-stream ::collector/output-stream
         websockets* ::rtng/websockets*} req]
    (-> (md/chain
          (http/websocket-connection req {:raw-stream? true})
          (fn [socket]
            (swap! websockets* conj socket)
            (ms/on-closed socket (fn clean []
                                     (swap! websockets* disj socket)))
            (ms/connect source-stream socket)
            socket))
        (md/catch (fn [_]
                    {:status 400
                     :headers {"content-type" "application/text"}
                     :body "Expected a websocket request."})))))
