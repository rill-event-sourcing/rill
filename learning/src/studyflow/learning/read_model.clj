(ns studyflow.learning.read-model)

(def empty-model {})

(defn index-course
  [course]
  (assoc course :sections-by-id
         (into {} (map #(vector (:id %) %) (mapcat :sections (:chapters course))))))

(defn set-student-section-status
  [model section-id student-id status]
  (assoc-in model [:section-statuses section-id student-id] status))

(defn set-course
  [model id material]
  (-> model 
      (assoc-in [:courses id] (index-course material))
      (assoc-in [:course-ids (:name material)] id)))

(defn remove-course
  [model id]
  (update-in model [:courses] #(dissoc % id)))

(defn get-course
  [model id]
  (get-in model [:courses id]))

(defn section-leaf
  [model section student-id]
  (-> section
      (select-keys [:id :title])
      (assoc :status (get-in model [:section-statuses (:id section) student-id]))))

(defn chapter-tree
  [model chapter student-id]
  {:id (:id chapter)
   :title (:title chapter)
   :sections (mapv #(section-leaf model % student-id) (:sections chapter))})

(defn course-tree
  [model course-id student-id]
  (let [course (get-course model course-id)]
    {:name (:name course)
     :id (:id course)
     :chapters (mapv #(chapter-tree model % student-id) (:chapters course))}))

(defn get-section
  [course section-id]
  (get-in course [:sections-by-id section-id]))

(defn get-question
  [section question-id]
  (some
   (fn [{:keys [id] :as question}]
     (when (= id question-id)
       question))
   (:questions section)))

(defn set-student
  [model student-id student]
  (assoc-in model [:students student-id] student))

(defn get-student
  [model student-id]
  (get-in model [:students student-id]))

(defn get-course-name
  "We currenly assume that there is only one course in the system"
  [model]
  (first (keys (:course-ids model))))

(defn get-course-id
  [model course-name]
  (get (:course-ids model) course-name))
