(ns studyflow.cli.validate-course-material-json
  (:require [cheshire.core :as json]
            [studyflow.learning.course-material :as material]
            [schema.utils :as utils]))

(defn -main
  [file]
  (let [r (material/parse-course-material* (json/parse-string (slurp file) true))]
    (when (utils/error? r)
      (prn (:error r))
      (System/exit 1))
    (println file " Ok")))


