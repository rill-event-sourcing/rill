(ns studyflow.school-administration.read-model)

(def empty-model {})

(defn set-aggregate-version
  [model aggregate-id version]
  (assoc-in model [:aggregate-versions aggregate-id] version))

(defn aggregate-version
  [model aggregate-id]
  (get-in model [:aggregate-versions aggregate-id]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; schools

(defn- decorate-department-for-school [model department]
  (assoc department
    :student-count (count (filter #(= (:id department) (:department-id %))
                                  (vals (:students model))))))

(defn- decorate-school [model school]
  (let [departments (filter #(= (:id school) (:school-id %))
                            (vals (:departments model)))
        department-ids (set (map :id departments))]
    (assoc school
      :departments (sort-by :name
                            (map (partial decorate-department-for-school model)
                                 departments))
      :licenses-sold (reduce + (filter #(not (nil? %))
                                       (map #(get-in % [:sales-data :licenses-sold]) departments)))
      :student-count (count (filter #(department-ids (:department-id %))
                                    (vals (:students model))))
      :version (aggregate-version model (:id school)))))

(defn list-schools [model]
  (map (partial decorate-school model)
       (sort-by :name (vals (:schools model)))))

(defn get-school [model id]
  (decorate-school model (get-in model [:schools id])))

(defn set-school
  [model id school]
  (assoc-in model [:schools id] school))

(defn set-school-name
  [model id name]
  (assoc-in model [:schools id :name] name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; departments

(defn- decorate-department [model department]
  (assoc department
    :school (decorate-school model (get-in model [:schools (:school-id department)]))
    :student-count (count (filter #(= (:id department) (:department-id %))
                                  (vals (:students model))))
    :version (aggregate-version model (:id department))))

(defn list-departments
  ([model]
     (map (partial decorate-department model)
          (sort-by :name (vals (:departments model))))))

(defn get-department
  [model id]
  (decorate-department model (get-in model [:departments id])))

(defn set-department
  [model id department]
  (assoc-in model [:departments id] department))

(defn set-department-name
  [model id name]
  (assoc-in model [:departments id :name] name))

(defn set-department-sales-data
  [model id licenses-sold status]
  (assoc-in model [:departments id :sales-data] {:licenses-sold licenses-sold
                                                 :status status}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; students

(defn- decorate-student [model student]
  (let [department (get-in model [:departments (:department-id student)])
        school (get-in model [:schools (:school-id department)])]
    (assoc student
      :school school
      :department department
      :version (aggregate-version model (:id student)))))

(defn list-students [model]
  (map (partial decorate-student model)
       (sort-by :full-name (vals (:students model)))))

(defn get-student [model id]
  (decorate-student model (get-in model [:students id])))

(defn set-student
  [model id student]
  (assoc-in model [:students id] student))

(defn set-student-full-name
  [model id name]
  (assoc-in model [:students id :full-name] name))

(defn set-student-email
  [model id email]
  (assoc-in model [:students id :email] email))

(defn set-student-department
  [model id department-id]
  (update-in model [:students id]
             (fn [student]
               (-> student
                   (assoc :department-id department-id)
                   (dissoc :class-name)))))

(defn set-student-class-name
  [model id name]
  (assoc-in model [:students id :class-name] name))


;; catchup

(defn caught-up
  [model]
  (assoc model :caught-up true))

(defn caught-up?
  [model]
  (boolean (:caught-up model)))
