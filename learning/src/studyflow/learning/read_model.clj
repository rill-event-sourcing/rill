(ns studyflow.learning.read-model)

(def empty-model {})

(defn index-course
  [course]
  (assoc course :sections-by-id
         (into {} (map #(vector (:id %) %) (mapcat :sections (:chapters course))))))

(defn set-course
  [model id material]
  (assoc-in model [:courses id] (index-course material)))

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
  {:name (:name course)
   :id (:id course)
   :chapters (mapv chapter-tree (:chapters course))})

(defn get-section
  [course section-id]
  (get-in course [:sections-by-id section-id]))

