(ns studyflow.teaching.read-model.event-handler
  (:require [rill.event-channel :as event-channel]
            [clojure.tools.logging :as log]
            [rill.message :as message]
            [studyflow.teaching.read-model :as m]))

(defmulti handle-event (fn [model event] (message/type event)))
(defmethod handle-event :default [model _] model)

;; student administration

(defmethod handle-event :studyflow.school-administration.student.events/Created
  [model {:keys [student-id full-name]}]
  (assoc-in model [:students student-id] {:id student-id, :full-name full-name}))

(defmethod handle-event :studyflow.school-administration.student.events/NameChanged
  [model {:keys [student-id full-name]}]
  (assoc-in model [:students student-id :full-name] full-name))

(defmethod handle-event :studyflow.school-administration.student.events/DepartmentChanged
  [model {:keys [student-id department-id]}]
  (let [old-class (-> (get-in model [:students student-id])
                      (select-keys [:department-id :class-name]))]
    (-> model
        (update-in [:students student-id] (fn [student]
                                            (-> student
                                                (assoc :department-id department-id)
                                                (dissoc :class-name))))
        (cond->
         (seq old-class)
         (update-in [:students-by-class old-class] disj student-id)))))

(defn update-student [model student-id f]
  (let [model (update-in model [:students student-id] f)
        class (-> (get-in model [:students student-id])
                  (select-keys [:department-id :class-name]))]
    (update-in model [:students-by-class class] (fnil conj #{}) student-id)))

(defmethod handle-event :studyflow.school-administration.student.events/ClassAssigned
  [model {:keys [student-id department-id class-name] :as event}]
  (update-student model student-id (fn [student]
                                     (assoc student
                                       :department-id department-id
                                       :class-name class-name))))

(defmethod handle-event :studyflow.school-administration.student.events/Imported
  [model {:keys [student-id full-name department-id class-name]}]
  (update-student model student-id (fn [student]
                                     {:id student-id
                                      :full-name full-name
                                      :department-id department-id
                                      :class-name class-name})))

(defmethod handle-event :studyflow.school-administration.school.events/Created
  [model {:keys [school-id name]}]
  (assoc-in model [:schools school-id] {:name name}))

(defmethod handle-event :studyflow.school-administration.school.events/NameChanged
  [model {:keys [school-id name]}]
  (assoc-in model [:schools school-id :name] name))

(defmethod handle-event :studyflow.school-administration.department.events/Created
  [model {:keys [department-id school-id name]}]
  (assoc-in model [:departments department-id] {:school-id school-id
                                                :name name}))

(defmethod handle-event :studyflow.school-administration.department.events/NameChanged
  [model {:keys [department-id name]}]
  (assoc-in model [:departments department-id :name] name))

;; finishing sections

(defmethod handle-event :studyflow.learning.section-test.events/Finished
  [model {:keys [section-id student-id]}]
  (->  model
       (update-in [:students student-id :finished-sections] (fnil conj #{}) section-id)
       (update-in [:students student-id :section-status] assoc section-id :finished)))

(defmethod handle-event :studyflow.learning.section-test.events/Created
  [model {:keys [section-id student-id]}]
  (update-in model [:students student-id :section-status] assoc section-id :in-progress))

(defmethod handle-event :studyflow.learning.section-test.events/Stuck
  [model {:keys [section-id student-id]}]
  (update-in model [:students student-id :section-status] assoc section-id :stuck))

(defmethod handle-event :studyflow.learning.section-test.events/Unstuck
  [model {:keys [section-id student-id]}]
  (update-in model [:students student-id :section-status] assoc section-id :in-progress))

(defmethod handle-event :studyflow.learning.entry-quiz.events/Passed
  [model {:keys [student-id course-id]}]
  (update-in model [:students student-id :course-entry-quiz-passed] (fnil conj #{}) course-id))

;; student time spend tracking
(defn end-time-spend [model {:keys [student-id] :as event}]
  ;; truncate the idle time period for whatever you were doing before
  (-> model
      (update-in [:students student-id :dashboard-time-spend] m/end-time-spend event)
      (update-in [:students student-id :entry-quiz-time-spend] m/end-time-spend event)
      (update-in [:students student-id :section-time-spend]
                 (fn [section-id+time-spend]
                   (zipmap (keys section-id+time-spend)
                           (map #(m/end-time-spend % event) (vals section-id+time-spend)))))))

(defn add-time-spend-section-test [model {:keys [section-id student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :section-time-spend section-id] m/add-time-spend event)))

(defmethod handle-event :studyflow.learning.section-test.events/QuestionAssigned
  [model event]
  (add-time-spend-section-test model event))

(defmethod handle-event :studyflow.learning.section-test.events/AnswerRevealed
  [model event]
  (add-time-spend-section-test model event))

(defmethod handle-event :studyflow.learning.section-test.events/QuestionAnsweredCorrectly
  [model event]
  (add-time-spend-section-test model event))

(defmethod handle-event :studyflow.learning.section-test.events/QuestionAnsweredIncorrectly
  [model event]
  (add-time-spend-section-test model event))

(defn add-time-spend-entry-quiz [model {:keys [student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :entry-quiz-time-spend] m/add-time-spend event)))

(defmethod handle-event :studyflow.learning.entry-quiz.events/Started
  [model event]
  (add-time-spend-entry-quiz model event))

(defmethod handle-event :studyflow.learning.entry-quiz.events/InstructionsRead
  [model event]
  (add-time-spend-entry-quiz model event))

(defmethod handle-event :studyflow.learning.entry-quiz.events/QuestionAnsweredCorrectly
  [model event]
  (add-time-spend-entry-quiz model event))

(defmethod handle-event :studyflow.learning.entry-quiz.events/QuestionAnsweredIncorrectly
  [model event]
  (add-time-spend-entry-quiz model event))

(defmethod handle-event :studyflow.learning.tracking.events/DashboardNavigated
  [model {:keys [student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :dashboard-time-spend] m/add-time-spend event)))

(defmethod handle-event :studyflow.learning.tracking.events/EntryQuizNavigated
  [model {:keys [student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :entry-quiz-time-spend] m/add-time-spend event)))

(defmethod handle-event :studyflow.learning.tracking.events/SectionExplanationNavigated
  [model {:keys [student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :entry-quiz-time-spend] m/add-time-spend event)))

(defmethod handle-event :studyflow.learning.tracking.events/SectionTestNavigated
  [model {:keys [student-id] :as event}]
  (-> model
      (end-time-spend event)
      (update-in [:students student-id :entry-quiz-time-spend] m/add-time-spend event)))

;; course material
(defn update-material [model course-id material]
  (let [remedial-sections-for-course (->> material
                                          :chapters
                                          (filter :remedial)
                                          (mapcat :sections)
                                          (map :id)
                                          set)
        all-sections (into (get model :all-sections #{})
                           (->> (:chapters material)
                                (mapcat :sections)))
        meijerink-criteria (->> all-sections
                                (mapcat :meijerink-criteria)
                                set)
        entry-quiz-meijerink-criteria (reduce into #{} (for [ch (:chapters material)
                                                             :when (:remedial ch)
                                                             s (:sections ch)]
                                                         (:meijerink-criteria s)))
        domains (->> all-sections
                     (mapcat :domains)
                     set)
        chapter-sections (->> (for [chapter (:chapters material)]
                                [(:id chapter) (map (fn [section]
                                                      {:id (:id section)
                                                       :title (:title section)}) (:sections chapter))])
                              (into {}))]
    (-> model
        (assoc-in [:courses course-id]
                  (-> material
                      (assoc :remedial-sections-for-course remedial-sections-for-course
                             :entry-quiz-meijerink-criteria entry-quiz-meijerink-criteria
                             :chapter-sections chapter-sections)))
        (assoc :all-sections all-sections
               :meijerink-criteria meijerink-criteria
               :domains domains))))

(defn strip-course-material [material]
  (let [strip-section (fn [section]
                        (dissoc section :questions :subsections :line-input-fields))
        strip-section-list (fn [sections]
                             (mapv strip-section sections))
        strip-chapter (fn [chapter]
                        (update-in chapter [:sections] strip-section-list))
        strip-chapter-list (fn [chapters]
                             (mapv strip-chapter chapters))
        strip-course (fn [course]
                       (update-in course [:chapters] strip-chapter-list))]
    (-> (strip-course material)
        (dissoc :entry-quiz))))

(defmethod handle-event :studyflow.learning.course.events/Published
  [model {:keys [course-id material]}]
  (update-material model course-id (strip-course-material material)))

(defmethod handle-event :studyflow.learning.course.events/Updated
  [model {:keys [course-id material]}]
  (update-material model course-id (strip-course-material material)))

;; teachers

(defmethod handle-event :studyflow.school-administration.teacher.events/Created
  [model {:keys [teacher-id full-name department-id]}]
  (assoc-in model [:teachers teacher-id] {:department-id department-id :full-name full-name :teacher-id teacher-id}))

(defmethod handle-event :studyflow.school-administration.teacher.events/DepartmentChanged
  [model {:keys [teacher-id department-id]}]
  (-> model
      (assoc-in [:teachers teacher-id :department-id] department-id)
      (update-in [:teachers teacher-id] dissoc :classes)))

(defmethod handle-event :studyflow.school-administration.teacher.events/ClassAssigned
  [model {:keys [teacher-id department-id class-name] :as event}]
  (update-in model [:teachers teacher-id :classes] (fnil conj #{}) {:department-id department-id
                                                                    :class-name class-name}))

(defmethod handle-event :studyflow.school-administration.teacher.events/ClassUnassigned
  [model {:keys [teacher-id department-id class-name]}]
  (update-in model [:teachers teacher-id :classes] disj {:department-id department-id
                                                         :class-name class-name}))

;; ready for display

(defmethod handle-event ::event-channel/CaughtUp
  [model _]
  (m/caught-up model))
