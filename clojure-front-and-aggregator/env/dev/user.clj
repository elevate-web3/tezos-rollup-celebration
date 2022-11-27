(ns user
  (:require [aleph.tcp :as tcp]
            [clj-commons.byte-streams :as bs]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.repl :as tools-repl]
            [rollup.server.tick :as tick]
            [orchestra.spec.test :as st]
            [juxt.clip.repl :as clip-repl]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
            [rollup.system :as sys]
            [shadow.cljs.devtools.api :as shadow]
            [rollup.server.collector :as collector]
            [shadow.cljs.devtools.server :as shadow-server]
            [clojure.core.async :as a]
            [rollup.server.util :as u]))

(set! *warn-on-reflection* true)

(tools-repl/set-refresh-dirs "env/dev" "src")

(clip-repl/set-init! sys/get-system-config)

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

  )

(comment

  (def loop-ch-cleaning-fn
    (let [continue?* (atom true)
          ;; ---
          {stream-ch ::u/chan
           clean-tcp-stream ::u/clean-fn}
          (collector/listen-collector {::collector/host "localhost"
                                       ::collector/port 1234})
          ;; ---
          {tick-ch ::u/chan
           clean-tick ::u/clean-fn}
          (tick/start-ticking {::tick/ms-time 1000})]
      (a/go-loop [byte-count 0]
        (if (true? @continue?*)
          (let [[val ch] (a/alts! [stream-ch tick-ch])]
            (if (identical? ch tick-ch)
              ;; tick event
              (do (println (java.util.Date.) ": " byte-count)
                  (recur byte-count))
              ;; else byte msg
              (recur (-> val #_u/throw-on-err count (+ byte-count)))))
          (println "Last count: " byte-count)))
      (fn clean []
        (if (true? @continue?*)
          (do (reset! continue?* false)
              (clean-tcp-stream)
              (clean-tick)
              :ok)
          :already-stopped))))

  (loop-ch-cleaning-fn)

  )
