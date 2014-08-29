(ns studyflow.teaching.read-model
  (:require [clojure.set :refer [intersection]]
            [clojure.string :as str]))

(def empty-model {})

(defn- all-section-ids [model]
  (->>
   (vals (:courses model))
   (mapcat :chapters)
   (mapcat :sections)
   (map :id)
   set))

(defn- decorate-student-completion [model student]
  (let [all (all-section-ids model)
        finished (intersection all (:finished-sections student))]
    (assoc student
      :total-completion {:finished (count finished)
                         :total (count all)})))

(defn students-for-class [model class]
  (map (partial decorate-student-completion model)
       (filter (fn [student]
                 (and (= (:department-id student) (:department-id class))
                      (= (:class-name student) (:name class))))
               (vals (:students model)))))

(defn- decorate-class-completion [model class]
  (let [total-completions (map :total-completion (students-for-class model class))
        sumf (fn [key] (reduce + (map key total-completions)))]
    (assoc class
      :total-completion {:finished (sumf :finished)
                         :total (sumf :total)})))

(defn classes [model]
  (->> (vals (:students model))
       (filter :class-name)
       (map #(select-keys % [:department-id :class-name]))
       set
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
