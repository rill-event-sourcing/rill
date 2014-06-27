(ns studyflow.web.api.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [clout-link.route :refer [uri-for]]
            [rill.uuid :refer [new-id]]
            [ring.mock.request :refer [request]]
            [studyflow.events :refer [->CoursePublished]]
            [studyflow.learning.course-material :as material]
            [studyflow.learning.course-material-test :refer [read-example-json]]
            [studyflow.learning.read-model.event-handler :refer [init-model]]
            [studyflow.web.api.query :refer [make-request-handler
                                             wrap-read-model]]
            [studyflow.web.routes :as routes]))


(def material (material/parse-course-material (read-example-json)))
(def course-id (:id material))
(def model (init-model [(->CoursePublished (new-id) course-id material)]))

(deftest test-query-api
  (let [handler (make-request-handler (atom model))]
    (testing "course material hierarchy"
      (is (= (:id (:body (handler (request :get (uri-for routes/query-course-material (str course-id))))))
             course-id)))

    (testing "section queries"
      (let [chapter (-> material :chapters first)
            chapter-id (:id chapter)
            section-id (-> chapter :sections first :id)]
        (assert section-id)
        (let [resp (handler (request :get (uri-for routes/query-section (str course-id) (str chapter-id) (str section-id))))]
          (is resp)
          (is (= (:status resp) 200))
          (is (= (:id (:body resp))
                 section-id)))))))

(deftest test-wrap-read-model
  (let [read-model {:read :model}
        h (fn [r] (:read-model r))
        wrapped (wrap-read-model h (atom read-model))]
    (is (= read-model (wrapped {})))))
