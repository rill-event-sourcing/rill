(ns studyflow.teaching.read-model-test
  (:require [clojure.tools.logging :as log]
            [clojure.test :refer [deftest is testing]]
            [studyflow.teaching.read-model :refer :all]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.course.events :as course]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.entry-quiz.events :as entry-quiz]
            [studyflow.school-administration.department.events :as department]
            [studyflow.school-administration.student.events :as student]
            [studyflow.school-administration.teacher.events :as teacher]
            [studyflow.teaching.read-model.event-handler :refer [handle-event]]))

(defn load-model [model & events] (reduce handle-event model events))

(def model {})

(def model-with-fred
  (load-model model
              (student/created "fred" "Fred Flintstone")))

(def model-with-fred-and-department
  (load-model model-with-fred
              (department/created "boulder" "bedrock" "Boulder road")
              (student/department-changed "fred" "boulder")))

(def model-with-fred-and-department-and-class
  (load-model model-with-fred-and-department
              (student/class-assigned "fred" "boulder" "1A")))

(def model-with-fred-and-teacher
  (load-model model-with-fred-and-department-and-class
              (teacher/created "teacher" "boulder" "Mr. Slate")
              (teacher/class-assigned "teacher" "boulder" "1A")))

(def model-with-fred-and-barney-in-same-class
  (load-model model-with-fred-and-teacher
              (student/created "barney" "Barney Rubble")
              (student/department-changed "barney" "boulder")
              (student/class-assigned "barney" "boulder" "1A")))

(def model-with-fred-barney-and-wilma-in-other-class
  (load-model model-with-fred-and-barney-in-same-class
              (student/created "wilma" "Wilma Flintstone")
              (student/department-changed "wilma" "boulder")
              (student/class-assigned "wilma" "boulder" "1B")))

(def model-fred-moved-to-vegas
  (load-model model-with-fred-barney-and-wilma-in-other-class
              (department/created "vegas" "bedrock" "Rock Vegas")
              (student/department-changed "fred" "vegas")))

(deftest students-and-classes
  (let [teacher (first (vals (:teachers model-with-fred-and-teacher)))
        students-by-class (fn [model teacher]
                            (->> (classes model teacher)
                                 (map #(vector (:id %) (->> (students-for-class model %) (map :full-name) set)))
                                 (into {})))]
    (testing "no students"
      (is (empty? (classes model teacher))))
    (testing "one student without departments"
      (is (empty? (classes model-with-fred teacher))))
    (testing "one student with departments without classes"
      (is (empty? (classes model-with-fred-and-department teacher))))
    (testing "one students with departments with classes"
      (is (= 1 (count (classes model-with-fred-and-teacher teacher))))
      (is (= {"bedrock|boulder|1A" #{"Fred Flintstone"}}
             (students-by-class model-with-fred-and-department-and-class teacher))))
    (testing "two students in the same class"
      (is (= 1 (count (classes model-with-fred-and-barney-in-same-class teacher))))
      (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}}
             (students-by-class model-with-fred-and-barney-in-same-class teacher))))
    (testing "three students; two in one, the other in another class"
      (is (= 1 (count (classes model-with-fred-barney-and-wilma-in-other-class teacher))))
      (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}}
             (students-by-class model-with-fred-barney-and-wilma-in-other-class teacher))))
    (let [model (load-model model-with-fred-barney-and-wilma-in-other-class
                            (teacher/class-assigned "teacher" "boulder" "1B"))
          teacher (first (vals (:teachers model)))]
      (testing "three students; two in one, the other in another class all same teacher"
        (is (= 2 (count (classes model teacher))))
        (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}
                "bedrock|boulder|1B" #{"Wilma Flintstone"}}
               (students-by-class model teacher))))
      (testing "three students; two different departments"
        (is (= 2 (count (classes model-fred-moved-to-vegas teacher))))
        (is (= {"bedrock|boulder|1A" #{"Barney Rubble"}
                "bedrock|boulder|1B" #{"Wilma Flintstone"}}
               (students-by-class model-fred-moved-to-vegas teacher)))))))

(def course {:chapters [{:remedial true
                         :sections [{:id "section-1"
                                     :meijerink-criteria #{"A" "B"}
                                     :domains #{"Getallen" "Verhoudingen"}}
                                    {:id "section-2"
                                     :meijerink-criteria #{"B"}
                                     :domains #{"Meetkunde"}}]}
                        {:sections [{:id "section-3"
                                     :meijerink-criteria #{"C"}
                                     :domains #{"Meetkunde" "Verbanden"}}]}]})

(def model-with-fred-barney-and-course
  (load-model model-with-fred-and-barney-in-same-class
              (course/published "course" course)))

(deftest completion
  (let [teacher (first (vals (:teachers model-with-fred-and-teacher)))]
    (testing "meijering-criteria"
      (is (= #{"A" "B" "C"}
             (meijerink-criteria model-with-fred-barney-and-course))))
    (testing "domains"
      (is (= #{"Getallen" "Verhoudingen" "Meetkunde" "Verbanden"}
             (domains model-with-fred-barney-and-course))))
    (testing "class completion without course material"
      (let [model model-with-fred-and-barney-in-same-class]
        (is (not (seq (:completion (first (classes model-with-fred-and-barney-in-same-class teacher))))))))
    (testing "one finished section without course material"
      (let [model (load-model model-with-fred-and-department-and-class
                              (section-test/finished "section-1" "fred"))]
        (is (not (seq (:completion (first (classes model teacher))))))))
    (testing "one finished section with three section course material for two students"
      (let [model (load-model model-with-fred-barney-and-course
                              (section-test/finished "section-1" "fred"))]
        (is (= {"A" {:all {:finished 1, :total 2}
                     "Getallen" {:finished 1, :total 2}
                     "Verhoudingen" {:finished 1, :total 2}
                     "Meetkunde" {:finished 0, :total 0}
                     "Verbanden" {:finished 0, :total 0}}
                "B" {:all {:finished 1, :total 4}
                     "Getallen" {:finished 1, :total 2}
                     "Verhoudingen" {:finished 1, :total 2}
                     "Meetkunde" {:finished 0, :total 2}
                     "Verbanden" {:finished 0, :total 0}}
                "C" {:all {:finished 0, :total 2}
                     "Getallen" {:finished 0, :total 0}
                     "Verhoudingen" {:finished 0, :total 0}
                     "Meetkunde" {:finished 0, :total 2}
                     "Verbanden" {:finished 0, :total 2}}}
               (:completion (first (classes model teacher)))))))
    (testing "remedial-sections-for-courses"
      (is (= #{"section-1" "section-2"}
             (remedial-sections-for-courses model-with-fred-barney-and-course #{"course"}))))
    (testing "one entry quiz passed with three section course material for two students"
      (let [model (load-model model-with-fred-barney-and-course
                              (entry-quiz/passed "course" "fred"))]
        (is (= {"A" {:all {:finished 1, :total 2}
                     "Getallen" {:finished 1, :total 2}
                     "Verhoudingen" {:finished 1, :total 2}
                     "Meetkunde" {:finished 0, :total 0}
                     "Verbanden" {:finished 0, :total 0}}
                "B" {:all {:finished 2, :total 4}
                     "Getallen" {:finished 1, :total 2}
                     "Verhoudingen" {:finished 1, :total 2}
                     "Meetkunde" {:finished 1, :total 2}
                     "Verbanden" {:finished 0, :total 0}}
                "C" {:all {:finished 0, :total 2}
                     "Getallen" {:finished 0, :total 0}
                     "Verhoudingen" {:finished 0, :total 0}
                     "Meetkunde" {:finished 0, :total 2}
                     "Verbanden" {:finished 0, :total 2}}}
               (:completion (first (classes model teacher)))))))))
