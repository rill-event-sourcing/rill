(ns studyflow.teaching.read-model
  (:require [clojure.tools.logging :as log]
            [clojure.set :refer [intersection]]
            [clojure.string :as str]))

(def empty-model {})

(defn- all-sections [model]
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

(defn- decorate-student-completion [model student]
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

(defn students-for-class [model class]
  (let [class-lookup (select-keys class [:department-id :class-name])]
    (for [student-id (get-in model [:students-by-class class-lookup])]
      (->> (get-in model [:students student-id])
           (decorate-student-completion model)))))

(defn decorate-class-completion [model class]
  (let [domains (domains model)
        student-completions (->> (students-for-class model class)
                                 (map :completion))
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
