(ns studyflow.school-administration.read-model)

(defn set-student
  [model id student]
  (assoc-in model [:students id] student))

(defn set-student-full-name
  [model id name]
  (assoc-in model [:students id :full-name] name))

(defn set-student-credentials
  [model id credentials]
  (assoc-in model [:students id :credentials] credentials))

(defn set-aggregate-version
  [model aggregate-id version]
  (assoc-in model [:aggregate-versions aggregate-id] version))

(defn aggregate-version
  [model aggregate-id]
  (get-in model [:aggregate-versions aggregate-id]))

(defn list-students
  "sequence of all the students in the system"
  [model]
  (map #(assoc % :version (aggregate-version model (:id %)))
       (sort-by :full-name (vals (:students model)))))
