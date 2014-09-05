(ns studyflow.teaching.read-model
  (:require [clojure.set :refer [intersection]]
            [clojure.string :as str]))

(def empty-model {})

(defn- all-sections [model]
  (->>
   (vals (:courses model))
   (mapcat :chapters)
   (mapcat :sections)))

(defn meijerink-criteria [model]
  (->>
   (all-sections model)
   (mapcat :meijerink-criteria)
   set))

(defn domains [model]
  (->>
   (all-sections model)
   (mapcat :domains)
   set))

(defn remedial-sections-for-courses [model courses]
  (->>
   (select-keys (:courses model) courses)
   vals
   (mapcat :chapters)
   (filter :remedial)
   (mapcat :sections)
   (map :id)
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
  (->> (vals (:students model))
       (filter (fn [student]
                 (and (= (:department-id student) (:department-id class))
                      (= (:class-name student) (:name class)))))
       (map (partial decorate-student-completion model))))

(defn- decorate-class-completion [model class]
  (let [domains (domains model)
        completions-f (fn [scope domain]
                        (->> (students-for-class model class)
                             (map :completion)
                             (map #(get-in % [scope domain]))))
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
  (->> (vals (:students model))
       (filter :class-name)
       (map #(select-keys % [:department-id :class-name]))
       set
       (intersection (:classes teacher))
       (map #(let [name (:class-name %)
                   department-id (:department-id %)
                   department (get-in model [:departments department-id])
                   school-id (:school-id department)
                   school (get-in model [:schools school-id])]
               {:id (str school-id "|" department-id "|" name)
                :name name
                :full-name (str/join " - " [(:name school) (:name department) name])
                :department-id department-id
                :department-name (:name department)
                :school-id school-id
                :school-name (:name school)}))
       (map (partial decorate-class-completion model))))

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
