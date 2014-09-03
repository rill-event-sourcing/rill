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
   (mapcat :domain)
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
  (let [sections (all-sections model)
        finished (into (set (:finished-sections student))
                       (remedial-sections-for-courses model (:course-entry-quiz-passed student)))
        mk-p (fn [v] #((set (:meijerink-criteria %)) v))
        mapping (reduce (fn [m v] (assoc m v (mk-p v)))
                        {:total identity}
                        (meijerink-criteria model))]
    (assoc student
      :completion (reduce (fn [m [v p]]
                            (let [ids (->> sections (filter p) (map :id) set)]
                              (assoc m v {:finished (count (intersection ids finished))
                                          :total (count ids)})))
                          {}
                          mapping))))

(defn students-for-class [model class]
  (->> (vals (:students model))
       (filter (fn [student]
                 (and (= (:department-id student) (:department-id class))
                      (= (:class-name student) (:name class)))))
       (map (partial decorate-student-completion model))))

(defn- decorate-class-completion [model class]
  (let [completions-f (fn [scope] (->> (students-for-class model class)
                                       (map :completion)
                                       (map #(get % scope))))
        sumf (fn [scope key] (reduce + (map key (completions-f scope))))]
    (assoc class
      :completion (reduce #(assoc %1
                             %2
                             {:finished (sumf %2 :finished)
                              :total (sumf %2 :total)})
                          {}
                          (into [:total] (meijerink-criteria model))))))

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
