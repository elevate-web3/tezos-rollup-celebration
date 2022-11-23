(ns user
  (:require [clojure.tools.namespace.repl :as tools-repl]
            [juxt.clip.repl :as clip-repl]
            [rollup.system :as sys]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]))

(set! *warn-on-reflection* true)

(tools-repl/set-refresh-dirs "env/dev" "src")

(clip-repl/set-init! sys/get-system-config)

(defn start []
  (clip-repl/start)
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

  )
