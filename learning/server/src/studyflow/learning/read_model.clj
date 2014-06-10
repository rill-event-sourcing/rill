(ns studyflow.learning.read-model)

(def empty-model {})

(defn set-course
  [model id material]
  (assoc-in model [:courses id] material))

(defn remove-course
  [model id]
  (update-in model [:courses] #(dissoc % id)))

(defn get-course
  [model id]
  (get-in model [:courses id]))

(defn section-leaf
  [section]
  (select-keys section [:id :title]))

(defn chapter-tree
  [chapter]
  {:id (:id chapter)
   :title (:title chapter)
   :sections (mapv section-leaf (:sections chapter))})

(defn course-tree
  [course]
  {:title (:title course)
   :id (:id course)
   :chapters (mapv chapter-tree (:chapters course))})


(def +current-model+
  "The 'current' version of the read model"
  (atom {}))

