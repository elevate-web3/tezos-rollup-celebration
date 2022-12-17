(ns user
  (:require [aleph.tcp :as tcp]
            [clj-commons.byte-streams :as bs]
            [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.repl :as tools-repl]
            [juxt.clip.repl :as clip-repl]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [orchestra.spec.test :as st]
            [rollup.server.collector :as collector]
            [rollup.server.util :as u]
            [rollup.system :as sys]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]))

(set! *warn-on-reflection* true)

(tools-repl/set-refresh-dirs "env/dev" "src")

(clip-repl/set-init! #(sys/get-system-config {}))

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

  (refresh-and-restart)

  (do (stop)
      (tools-repl/refresh))

  )

(comment
  (->> (let [max-col 20]
         (for [row (range 5)
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
