(ns rollup.server.routes
  (:require [manifold.deferred :as md]
            [reitit.ring :as rr]
            [ring.middleware.defaults :as rd]
            [rollup.server.handlers.public :as public-h]))

(defn wrap-ring-middleware
  "Wraps an async capable ring middleware and exposes an Aleph-compliant one.
   This is useful to wrap ring defaults for example.
   (require '[ring.middleware.defaults :as rd])
   (def wrap-defaults
     (wrap-ring-middleware #(rd/wrap-defaults % rd/site-defaults)))
  "
  [ring-middleware]
  (fn manifold-middleware [handler]
    (let [handler' (ring-middleware (fn arity-3-to-1 [request respond raise]
                                      (-> (handler request)
                                          (md/chain respond)
                                          (md/catch raise))))]
      (fn manifold-handler [request]
        (let [response (md/deferred)]
          (handler' request #(md/success! response %) #(md/error! response %))
          response)))))

;; To be able to recompile handlers on the fly on dev
;; h for handler
(defmacro h [handler-sym]
  (let [env (System/getenv "ROLLUP_ENV")]
    (if (contains? #{"prod" "local-prod" "testing"} env)
      handler-sym
      `(fn ~(-> handler-sym name symbol) [req#]
         (~handler-sym req#)))))

(defn make-routes [_config]
  ["" ;; {:middleware [[my-middleware config]]}
   ["/" {:name :home-page
         :get (h public-h/home-page)}]
   ["/data-stream" {:name :data-stream
                    :get (h public-h/data-stream)}]])

(defn make-ring-reitit-router []
  (let [ring-config {:params    {:urlencoded true
                                 :multipart  true
                                 :nested     true
                                 ;; :keywordize true
                                 }
                     ;; :session   {:flash true
                     ;;             :cookie-attrs {:http-only true, :same-site :strict}}
                     :security  {:anti-forgery   true
                                 :frame-options  :sameorigin
                                 :content-type-options :nosniff}
                     :static    {:resources "public"}
                     :responses {:not-modified-responses true
                                 :absolute-redirects     false
                                 :content-types          true
                                 :default-charset        "utf-8"}}
        wrap-defaults-mdlw (wrap-ring-middleware
                             (fn middleware [handler]
                               (rd/wrap-defaults handler ring-config)))]
    (-> (make-routes {})
        rr/router
        ;; https://github.com/metosin/reitit/blob/master/doc/ring/slash_handler.md
        (rr/ring-handler
          (rr/routes
            (rr/redirect-trailing-slash-handler {:method :strip})
            (rr/create-default-handler
              {:not-found (fn not-found [_req]
                            {:status  404
                             :headers {"Content-Type" "text/html"}
                             :body "<h1>404 not found</h1>"})})))
        wrap-defaults-mdlw)))
