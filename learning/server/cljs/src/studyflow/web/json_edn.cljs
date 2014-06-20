(ns studyflow.web.json-edn
  (:require [clojure.walk :as walk]))

(defn json->edn [json]
  (walk/keywordize-keys json))
