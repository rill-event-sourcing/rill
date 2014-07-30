(ns studyflow.web.start
  (:require [studyflow.web.routes :as routes]
            [clout-link.route :refer [handle uri-for]]
            [studyflow.learning.read-model :refer [get-course-id]]
            [ring.util.response :refer [redirect]]
            [clojure.tools.logging :as log]))


(def handler
  (handle routes/get-start
          (fn [{model :read-model}]
            (redirect (uri-for routes/get-course-page (get-course-id model))))))

(defn make-request-handler
  [read-model]
  (fn [r]
    (handler (assoc r :read-model @read-model))))
