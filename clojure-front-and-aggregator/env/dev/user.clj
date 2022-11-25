(ns user
  (:require [aleph.tcp :as tcp]
            [clj-commons.byte-streams :as bs]
            [clojure.spec.alpha :as s]
            [clojure.tools.namespace.repl :as tools-repl]
            [juxt.clip.repl :as clip-repl]
            [manifold.deferred :as md]
            [manifold.stream :as ms]
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

(comment
  ;; On a terminal, create a file "test.txt" with some text in it.
  ;; Then run the following command : nc -l -p 1234 < ./test.txt
  ;; And then run the following expression with a REPL
  (md/chain
    (tcp/client {:port 1234 :host "localhost"})
    (fn [stream]
      (md/loop []
        (md/chain
          (ms/take! stream ::drained)
          (fn [msg]
            (if (identical? msg ::drained)
              (println "End of stream")
              (do (-> msg (bs/convert String) println)
                  (md/recur))))))))
  )
