(ns studyflow.json-tools
  (:require [clojure.string :as str]))

(defn key-from-json
  [key-str]
  (if (.startsWith key-str "_INPUT") ; HACK, HACK, HACK, TODO, TODO, TODO
    key-str
    (keyword (str/replace key-str \_ \-))))

(defn key-to-json
  [key-keyword]
  (str/replace (name key-keyword) \_ \-))
