(ns studyflow.teaching.read-model
  (:require [clj-time.core :as t]
            [clj-time.coerce :as time-coerce]
            [clojure.tools.logging :as log]
            [clojure.set :refer [intersection]]
            [clojure.string :as str]
            [rill.message :as message]))

(def empty-model {})

(defn all-sections [model]
  (get-in model [:all-sections]))

(defn meijerink-criteria [model]
  (get-in model [:meijerink-criteria]))

(defn domains [model]
  (get-in model [:domains]))

(defn remedial-sections-for-courses [model courses]
  (->>
   (select-keys (:courses model) courses)
   vals
   (mapcat :remedial-sections-for-course)
   set))

(defn decorate-student-completion [model student]
  (let [finished-ids (into (set (:finished-sections student))
                           (remedial-sections-for-courses model (:course-entry-quiz-passed student)))
        sections (all-sections model)
        completions #(let [ids (->> % (map :id) set)]
                       {:finished (count (intersection ids finished-ids))
                        :total (count ids)})]
    (assoc student
      :completion
      (into {}
            (map (fn [mc]
                   (let [sections (filter #(contains? (:meijerink-criteria %) mc) sections)]
                     {mc (into {:all (completions sections)}
                               (map (fn [domain]
                                      (let [sections (filter #(contains? (:domains %) domain) sections)]
                                        {domain (completions sections)}))
                                    (domains model)))}))
                 (meijerink-criteria model))))))

(defn decorate-student-time-spend [model student]
  (let [;; time spend on entry quiz counts as time spend in the
        ;; remedial chapters
        entry-quiz-criteria (->> (get-in model [:courses])
                                 first
                                 val
                                 :entry-quiz-meijerink-criteria
                                 set)
        time-per-criteria (zipmap (meijerink-criteria model)
                                  (for [criteria (meijerink-criteria model)]
                                    (if (contains? entry-quiz-criteria criteria)
                                      (get-in model [:students (:id student) :entry-quiz-time-spend :total-secs] 0)
                                      0)))
        total-per-criteria (reduce
                            (fn [acc section]
                              (reduce
                               (fn [acc criteria]
                                 (update-in acc [criteria]
                                            + (get-in model [:students (:id student) :section-time-spend (:id section) :total-secs] 0)))
                               acc
                               (:meijerink-criteria section)))
                            time-per-criteria
                            (all-sections model))]
    (assoc student
      :time-spend total-per-criteria)))

(defn students-for-class [model class]
  (let [class-lookup (select-keys class [:department-id :class-name])]
    (for [student-id (get-in model [:students-by-class class-lookup])]
      (get-in model [:students student-id]))))

(defn decorate-class-completion [model students class]
  (let [domains (domains model)
        student-completions (map :completion students)
        completions-f (fn [scope domain]
                        (map #(get-in % [scope domain]) student-completions))
        sumf (fn [scope domain key] (reduce +
                                            (->> (completions-f scope domain)
                                                 (map key )
                                                 (filter identity))))]
    (assoc class
      :completion (reduce (fn [m mc]
                            (reduce #(assoc-in %1 [mc %2]
                                               {:finished (sumf mc %2 :finished)
                                                :total (sumf mc %2 :total)})
                                    m
                                    (into [:all] domains)))
                          {}
                          (into [] (meijerink-criteria model))))))

(defn decorate-class-time-spend [model students class]
  (let [domains (domains model)
        student-time-spend (map :time-spend students)
        criteria (meijerink-criteria model)]
    (assoc class
      :time-spend
      (zipmap criteria
              (for [meijerink-criteria criteria]
                (if-let [student-time-spend (seq student-time-spend)]
                  (long (Math/floor (/ (reduce + (map #(get % meijerink-criteria 0) student-time-spend))
                                       (count student-time-spend))))
                  0))))))

(defn classes [model teacher]
  (let [teacher-id (:teacher-id teacher)]
    (->> (get-in model [:teachers teacher-id :classes])
         (map #(let [class-name (:class-name %)
                     department-id (:department-id %)
                     department (get-in model [:departments department-id])
                     school-id (:school-id department)
                     school (get-in model [:schools school-id])]
                 {:id (str school-id "|" department-id "|" class-name)
                  :class-name class-name
                  :full-name (str/join " - " [(:name school) (:name department) class-name])
                  :department-id department-id
                  :department-name (:name department)
                  :school-id school-id
                  :school-name (:name school)})))))

(defn students-who-finished-this-section [model students section]
  (filter
   (fn [student]
     (= (get-in model [:students (:id student) :section-status (:id section)]) :finished))
   students))

(defn total-number-of-finished-sections [model students sections]
  (reduce +
          0
          (map (fn [section]
                 (count (students-who-finished-this-section model students section)))
               sections)))

(defn chapter-completion [model students sections]
  {:total (* (count sections) (count students))
   :finished (total-number-of-finished-sections model students sections)})

(defn chapters-completion [model chapter-sections students]
  (zipmap (keys chapter-sections)
          (map (fn [sections] (chapter-completion model students sections))
               (vals chapter-sections))))

(defn students-status-for-section [model students section]
  (->> students
       (map
        (fn [student]
          (assoc student
            :status
            (get-in model [:students (:id student) :section-status (:id section)] :unstarted)
            :time-spend
            (get-in model [:students (:id student) :section-time-spend (:id section) :total-secs] 0))))
       (group-by :status)))

(defn sections-total-status [model students chapter-with-sections selected-section-id]
  (into {}
        (for [section chapter-with-sections]
          (let [students-status (students-status-for-section model students section)]
            [(:id section)
             (-> section
                 (merge (zipmap (keys students-status)
                                (map count (vals students-status))))
                 (cond->
                  (= selected-section-id (:id section))
                  (assoc :student-list students-status)))]))))

(defn chapter-list [model class chapter-id section-id]
  (let [material (val (first (:courses model)))
        students (students-for-class model class)
        chapters (get material :chapter-sections)
        chapter-with-sections (get-in chapters [chapter-id])]
    (assoc material
      :sections-total-status {chapter-id (sections-total-status model students chapter-with-sections section-id)}
      :chapters-completion (chapters-completion model chapters students))))

(defn get-teacher
  [model id]
  (get-in model [:teachers id]))

;; catchup

(defn caught-up
  [model]
  (assoc model :caught-up true))

(defn caught-up?
  [model]
  (boolean (:caught-up model)))

(def idle-time-secs (* 5 60))

(defn end-time-spend [current event]
  (when current
    (let [current-end (:end current)
          end (time-coerce/from-date (::message/timestamp event))
          overlap (if (t/before? end current-end)
                    (t/in-seconds (t/interval end current-end))
                    0)]
      (if (t/before? end current-end)
        {:start (:start current)
         :end end
         :total-secs
         (- (:total-secs current) overlap)}
        current))))

(defn add-time-spend [current event]
  (let [start (time-coerce/from-date (::message/timestamp event))
        end (t/plus start (t/seconds idle-time-secs))]
    {:start (:start current start)
     :end end
     :total-secs (+ (:total-secs current 0)
                    idle-time-secs)}))
