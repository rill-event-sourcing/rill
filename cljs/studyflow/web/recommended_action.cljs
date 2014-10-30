(ns studyflow.web.recommended-action
  (:require [studyflow.web.history :refer [history-link]]
            [studyflow.web.helpers :refer [section-explanation-link]]))

(defn not-finished? [element]
  (when-not (= (:status element) "finished")
    element))

(defn not-stuck? [section]
  (when-not (= (:status section) "stuck")
    section))

(defn first-recommendable-section [chapter]
  (some (comp not-stuck? not-finished?) (:sections chapter)))

(defn nonfinished-chapter-with-recommendable-sections [chapter]
  (when (and
         (not (= (:status chapter) "finished"))
         (first-recommendable-section chapter))
    chapter))

(defn first-recommendable-chapter [course]
  (some nonfinished-chapter-with-recommendable-sections (:chapters course)))

(defn recommended-action [cursor]
  (let [course (get-in cursor [:view :course-material])
        entry-quiz (:entry-quiz course)]
    (if (not (contains? #{"passed" "failed"} (:status entry-quiz)))
      {:title "Instaptoets"
       :link (history-link {:main :entry-quiz})
       :id (:id entry-quiz)}
      (let [chapter (first-recommendable-chapter course)
            section (first-recommendable-section chapter)]
        {:title (:title section)
         :id (:id section)
         :link (section-explanation-link cursor chapter section)} ))))

