(ns user
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.repl :as tools-repl]
            [flames.core :as flames]
            [juxt.clip.repl :as clip-repl]
            [orchestra.spec.test :as st]
            [rollup.system :as sys]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [rollup.server.util :as u]))

(set! *warn-on-reflection* true)

(tools-repl/set-refresh-dirs "env/dev" "src")

(s/check-asserts true)

(clip-repl/set-init! #(sys/get-system-config '("--stream-mockup=random"
                                               "--rows=2"
                                               "--columns=5"
                                               "--interval=40"
                                               "--msg-size=40")))

;; (clip-repl/set-init! #(sys/get-system-config '("--config=resources/collectors-example.json")))

(defn start []
  (clip-repl/start)
  (st/instrument)
  (println "Clip DEV system started")
  nil)

(defn stop []
  (clip-repl/stop)
  (println "Clip DEV system stopped")
  nil)

(defn reset []
  (stop)
  (start))

(defn cljs-repl
  ([]
   (cljs-repl :frontend))
  ([build-id]
   (shadow-server/start!)
   (shadow/watch build-id)
   (shadow/nrepl-select build-id)))

(defn refresh-and-restart []
  (stop)
  (tools-repl/refresh :after 'user/start))

(comment

  (start)

  (stop)

  (do (stop)
      (start))

  (refresh-and-restart)

  (do (stop)
      (tools-repl/refresh))

  (def flames (flames/start! {:port 54321, :host "localhost"}))
  (flames/stop! flames)

  )

(comment
  (->> (let [max-col 5]
         (for [row (range 2)
               col (range max-col)]
           {:host "localhost"
            :port (-> (* max-col row)
                      (+ col 1234))
            :row row
            :column col}))
       vec
       json/write-str
       (spit "resources/collectors-example.json"))
  )
