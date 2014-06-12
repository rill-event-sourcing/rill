(ns studyflow.json-tools
  (:require [clojure.string :as str]))

(defn key-from-json
  [key-str]
  (keyword (str/replace key-str \_ \-)))

(defn key-to-json
  [key-keyword]
  (str/replace (name key-keyword) \_ \-))

