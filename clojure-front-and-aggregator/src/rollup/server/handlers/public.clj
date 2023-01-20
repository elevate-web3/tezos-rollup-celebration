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
            [rollup.server.routing :as rtng]))

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
        [:style "--bs-progress-bar-transition: opacity 0s linear;"]]
       [:body {:style {:background site-background
                       :color "white"
                       :font-family "Poppins"}}
        [:nav.navbar.navbar-dark.navbar-expand {:style {:background-color tezos-grey-dark}}
         [:div.container-fluid
          [:a.navbar-brand {:href "#"}
           [:img {:style {:height "50px"}
                  :src "/images/TezosLogo_Horizontal_White.svg"}]]
          [:ul.navbar-nav
           (->> [{:text "DATA"
                  :link "#rollup-demo-data"}
                 {:text "VISUALIZATION"
                  :link "#my-canvas"}
                 {:text "EXPLAINATION"
                  :link "#rollup-demo-explanation"}
                 {:text "TEAMS"
                  :link "#rollup-demo-teams"}]
                (map (fn [{:keys [text link]}]
                       [:li.nav-item.fw-bold.pe-3
                        [:a.nav-link {:href link
                                      :style {:color "white"
                                              :font-size "1.25rem"}}
                         text]])))]]]
        ;; Data section
        [:div#rollup-demo-data.container-fluid.text-center
         [:div.row.row-cols-1.row-cols-sm-2
          [:div.col.py-5
           [:h1
            "Race to 120 millions transactions"]
           [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]]
          [:div.col.py-4.d-flex.justify-content-center.align-items-center
           [:div.flex-fill
            [:p
             [:span#transaction-count.h3 "0"]
             [:span.h3 "M"]
             [:br]
             "transactions out of a total of 12M"]
            [:div.d-inline-block.w-50
             [:div.progress {:style {:background-color tezos-grey-light}}
              [:div#progress-bar.progress-bar {:style {:width "5%"
                                                       :transition "opacity 0s linear"
                                                       :background-color tezos-grey-stealth}
                                               :role "progressbar",
                                               :aria-label "Basic example",
                                               :aria-valuenow "0",
                                               :aria-valuemin "0",
                                               :aria-valuemax "100"}]]]
            [:p.mt-4
             [:span.h3 "100000"]
             [:br]
             "Transactions per second"]]]]]
        ;; Visualization section
        [:canvas#my-canvas.d-block.w-100 {:width "2500"
                                          :height "2000"
                                          :style {:border "1px solid black"}}]
        ;; Explanation section
        [:div#rollup-demo-explanation.container-fluid.mt-5
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
        [:div#rollup-demo-teams.container-fluid.mt-5
         [:h1 "Teams"]
         [:div.row.row-cols-1.row-cols-sm-3.gy-3.mt-3
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:h3 "Nomadic Labs"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:h3 "TriliTech"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]
          [:div.col
           [:div.card {:style {:color "black"}}
            [:div.card-body
             [:h3 "Elevate"]
             [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]
             ]]]]]
        ;; Footer space
        [:div {:style {:height "50px"}}]
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
