(ns studyflow.learning.validate-course-material-json
  (:require [cheshire.core :as json]
            [studyflow.learning.course-material :as material]
            [schema.utils :as utils]
            [studyflow.json-tools :refer [key-from-json]]))

(defn validate-course-material
  [json-string]
  (let [r (material/parse-course-material* (json/parse-string json-string key-from-json))]
    (when (utils/error? r)
      (:error r))))

(defn -main
  [file]
  (when-let [err (validate-course-material (slurp file))]
    (prn err)
    (System/exit 1))
  (println file "ok"))
