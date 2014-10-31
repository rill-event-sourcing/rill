(ns studyflow.learning.read-model)

(def empty-model {})

(defn index-course
  [course]
  (assoc course :sections-by-id
         (into {} (map #(vector (:id %) %) (mapcat :sections (:chapters course))))))

(defn update-student-section-status
  [model section-id student-id current-status new-status]
  ;; a finished section will always be marked as such, even when
  ;; continuing after
  (update-in model [:section-statuses section-id student-id]
             (fn [old-status]
               (if (= old-status current-status)
                 new-status
                 old-status))))

(defn set-student-remedial-chapters-status
  [model course-id student-id status]
  (assoc-in model [:remedial-chapters-status course-id student-id] status))

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

(defn set-student-chapter-quiz-status [model chapter-id student-id status]
  (assoc-in model [:chapter-quiz-statuses chapter-id student-id] status))

(defn get-chapter-status [model chapter-id student-id]
  (get-in model [:chapter-statuses chapter-id student-id]))

(defn set-chapter-status [model chapter-id student-id status]
  (assoc-in model [:chapter-statuses chapter-id student-id] status))

(defn set-remedial-chapters-finished [model course-id student-id]
  (let [course (get-course model course-id)
        chapters (:chapters course)]
    (reduce (fn [model chapter]
              (if (:remedial chapter)
                (set-chapter-status model (:id chapter) student-id :finished)
                model))
            model chapters)))

(defn chapter-tree
  [model chapter student-id remedial-chapters-status]
  {:id (:id chapter)
   :title (:title chapter)
   :chapter-quiz {:number-of-questions (count (:chapter-quiz chapter))
                  :status (get-in model [:chapter-quiz-statuses (:id chapter) student-id])}
   :status (get-chapter-status model (:id chapter) student-id)
   :sections (mapv #(section-leaf model % student-id) (:sections chapter))})

(defn get-student-entry-quiz-status [model entry-quiz-id student-id]
  (get-in model [:entry-quiz-statuses student-id]))

(defn set-student-entry-quiz-status [model entry-quiz-id student-id status]
  (assoc-in model [:entry-quiz-statuses student-id] status))

(defn entry-quiz [model course-id student-id]
  (let [entry-quiz (:entry-quiz (get-course model course-id))
        student-status (get-student-entry-quiz-status model (:id entry-quiz) student-id)]
    (assoc entry-quiz
      :status student-status)))

(defn course-tree
  [model course-id student-id]
  (let [course (get-course model course-id)
        remedial-chapters-status (get-in model [:remedial-chapters-status course-id student-id])]
    {:name (:name course)
     :id (:id course)
     :chapters (mapv #(chapter-tree model % student-id remedial-chapters-status) (:chapters course))
     :entry-quiz (entry-quiz model (:id course) student-id)}))

(defn get-section
  [course section-id]
  (get-in course [:sections-by-id section-id]))

(defn get-chapter
  [course chapter-id]
  (first (filter (fn [chapter] (= chapter-id (:id chapter))) (:chapters course))))

(defn get-question
  [section question-id]
  (some
   (fn [{:keys [id] :as question}]
     (when (= id question-id)
       question))
   (:questions section)))

(defn get-chapter-quiz-question
  [chapter question-id]
  (let [all-questions (mapcat :questions (:chapter-quiz chapter))]
    (some (fn [{:keys [id] :as question}]
            (when (= id question-id)
              question))
          all-questions)))

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


;; catchup

(defn caught-up
  [model]
  (assoc model :caught-up true))

(defn caught-up?
  [model]
  (boolean (:caught-up model)))
