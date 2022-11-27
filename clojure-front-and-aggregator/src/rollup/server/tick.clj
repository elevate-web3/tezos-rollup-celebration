(ns rollup.server.tick
  (:require [clojure.core.async :as a]
            [orchestra.core :as _]
            [rollup.server.util :as u]
            [rollup.server.config :as c]
            [clojure.spec.alpha :as s]))

(s/def ::ms-time integer?)
;; Value passed to channel to indicate ticking time
(s/def ::tick any?)

(_/defn-spec start-ticking (s/keys :req [::u/chan ::u/clean-fn])
  [m (s/keys :req [::ms-time])]
  (let [c (a/chan)]
    (a/go-loop []
      (when (a/>! c ::tick)
        (a/<! (a/timeout (::ms-time m)))
        (recur)))
    {::u/chan c
     ::u/clean-fn (fn clean-tick! []
                    (a/close! c))}))
