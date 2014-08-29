(ns studyflow.teaching.read-model
  (:require [clojure.set :refer [intersection]]
            [clojure.string :as str]))

(def empty-model {})

(defn meijerink-criteria [model]
  (->>
   (vals (:courses model))
   (mapcat :chapters)
   (mapcat :sections)
   (mapcat :meijerink-criteria)
   set))

(defn- all-sections [model]
  (->>
   (vals (:courses model))
   (mapcat :chapters)
   (mapcat :sections)))

(defn- decorate-student-completion [model student]
  (let [sections (all-sections model)
        finished (:finished-sections student)
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
  (map (partial decorate-student-completion model)
       (filter (fn [student]
                 (and (= (:department-id student) (:department-id class))
                      (= (:class-name student) (:name class))))
               (vals (:students model)))))

(defn- decorate-class-completion [model class]
  (let [completions-f (fn [scope] (->> (students-for-class model class) (map :completion) (map #(get % scope))))
        sumf (fn [scope key] (reduce + (map key (completions-f scope))))]
    (assoc class
      :completion (into {} (map #(vector %
                                         {:finished (sumf % :finished)
                                          :total (sumf % :total)})
                                (into [:total] (meijerink-criteria model)))))))

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

;; catchup

(defn caught-up
  [model]
  (assoc model :caught-up true))

(defn caught-up?
  [model]
  (boolean (:caught-up model)))
