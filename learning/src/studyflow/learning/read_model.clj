(ns studyflow.learning.read-model)

(def empty-model {})

(defn set-course-material
  [model id material]
  (assoc-in model [:courses id] material))

(defn remove-course
  [model id]
  (update-in model [:courses] #(dissoc % id)))

(defn chapter-tree
  [model id]
  (let [chapter (get-chapter model id)]
    {:id id
     :title (:title chapter)
     :learning-steps (mapv (fn [id]
                             {:id id
                              :title (:title (get-learning-step model id))})
                           (:learning-step-ids chapter))}))

(defn get-course
  [model id]
  (get-in model [:courses id]))

(defn course-tree
  [model id]
  (let [course (get-course model id)]
    {:title (:title course)
     :id id
     :chapters (mapv (partial chapter-tree model) (:chapter-ids course))}))

