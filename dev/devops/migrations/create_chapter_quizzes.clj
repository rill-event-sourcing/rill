(ns devops.migrations.create-chapter-quizzes
  (:require [rill.repository :refer [retrieve-aggregate]]
            [rill.event-store :refer [retrieve-events append-events]]
            [rill.event-stream :refer [all-events-stream-id any-stream-version]]
            [rill.message :as message]
            [rill.handler :refer [notify-observers]]
            [studyflow.learning.section-test]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.chapter-quiz]
            [studyflow.learning.chapter-quiz.events :as chapter-quiz]))


(defn chapters-for-sections
  [material]
  (into {}
        (mapcat (fn [chapter]
                  (map (fn [section]
                         [(:id section) (:id chapter)])
                       (:sections chapter)))
                (:chapters material))))

(defn introduce-chapter-quizzes
  [repo course-id]
  (println "Loading course material...")
  (let [material (retrieve-aggregate repo course-id)
        section->chapter (chapters-for-sections material)
        fixup-event #(assoc %
                       :chapter-id (section->chapter (:section-id %))
                       :course-id course-id)]
    (println "Getting section-finished events")
    (doseq [e (->> (retrieve-events repo all-events-stream-id)
                   (filter #(= ::section-test/Finished (message/type %)))
                   (map fixup-event))]
      (println (message/timestamp e))
      (notify-observers repo e))))


