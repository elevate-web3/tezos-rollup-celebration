(ns rollup.server.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [rollup.server.mockup :as mockup]
            [clojure.tools.cli :as cli]
            [orchestra.core :as _]
            [clojure.java.io :as io]))

;; Time in ms
(s/def ::ticking-time integer?)

(def ^:const sliding-stream-buffer-size
  10000)

;; Path to a custom JSON file for collectors
(s/def ::json-config-path (s/and string?
                                 #(-> % str/blank? not)))
(s/def ::stream-mockup #{"static" "random"})
(s/def ::digits-only #(re-matches #"\d+" %))
(s/def ::rows ::digits-only)
(s/def ::columns ::digits-only)
(s/def ::interval ::digits-only)
(s/def ::msg-size ::digits-only)

(s/def ::options
  (s/or :json (s/keys :req [::json-config-path])
        :mockup (s/keys :req [::stream-mockup ::rows ::columns ::interval ::msg-size])))

(def cli-options-config
  (let [digit-msg "This option accepts digits only"]
    [[nil "--config=" "JSON config file for collectors"
      :id ::json-config-path
      :validate [#(s/valid? ::json-config-path %)
                 "JSON config file path cannot be empty"]]
     [nil "--stream-mockup=" "Mockup input streams. 'static' or 'random'"
      :id ::stream-mockup
      :validate [#(s/valid? ::stream-mockup %)
                 "Possible values for mockup are: 'static' or 'random'"]]
     [nil "--rows=" "Mockup rows"
      :id ::rows
      :validate [#(s/valid? ::rows %) digit-msg]]
     [nil "--columns=" "Mockup columns"
      :id ::columns
      :validate [#(s/valid? ::columns %) digit-msg]]
     [nil "--interval=" "Mockup interval in ms"
      :id ::interval
      :validate [#(s/valid? ::interval %) digit-msg]]
     [nil "--msg-size=" "Mockup message size (messages per batch)"
      :id ::msg-size
      :validate [#(s/valid? ::msg-size %) digit-msg]]]))

(_/defn-spec parse-cli-options ::options
  [args (s/coll-of string?)]
  (let [m (cli/parse-opts args cli-options-config)
        json-file (get-in m [:options ::json-config-path])]
    (cond
      (:errors m) (-> (str/join ".\n" (:errors m))
                      (Error.)
                      throw)
      ;; When mockup is activated, all mockup keys are required
      (::stream-mockup (:options m)) (when-not (s/assert (s/keys :req [::rows ::columns ::interval ::msg-size])
                                                         (:options m))
                                       (throw (Error. "Rows, columns, interval and message size options are required when activating mockup")))
      json-file (when-not (.exists (io/as-file json-file))
                  (throw (Error. (str "The config file " json-file " does not exist"))))
      :else (throw (Error. "No valid configuration to start the system")))
    (:options m)))

(comment
  (parse-cli-options '("--config=foo/dam.json" "--stream-mockup=static" "--mockup-rows=a" "--columns=b"))
  (cli/parse-opts '("--config=foo/dam.json" "--stream-mockup=static" "--mockup-rows=a" "--columns=b") cli-options-config)
  )
