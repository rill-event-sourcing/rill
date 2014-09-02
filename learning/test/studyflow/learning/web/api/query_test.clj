(ns studyflow.learning.web.api.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [clout-link.route :refer [uri-for]]
            [rill.uuid :refer [new-id]]
            [ring.mock.request :refer [request]]
            [studyflow.learning.web.routes :as routes]
            [clout-link.route :refer [uri-for]]
            [studyflow.learning.web :refer [wrap-read-model]]
            [studyflow.learning.web.api.query :as query]
            [studyflow.learning.read-model.event-handler :refer [init-model]]
            [studyflow.learning.course.fixture :as fixture] 
            [studyflow.learning.course.events :as events]))

(def material fixture/course-edn)
(def course-id (:id material))
(def model (init-model [(events/published course-id material)]))

(deftest test-query-api
  (let [handler (wrap-read-model query/handler (atom model))]
    (testing "course material hierarchy"
      (is (= (:id (:body (handler (request :get (uri-for routes/query-course-material (str course-id) (str (new-id)))))))
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
