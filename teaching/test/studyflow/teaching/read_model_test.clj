(ns studyflow.teaching.read-model-test
  (:require [clj-time.core :as t]
            [clj-time.coerce :as ct]
            [rill.message :as m]
            [clojure.tools.logging :as log]
            [clojure.test :refer [deftest is testing]]
            [studyflow.teaching.read-model :refer :all]
            [rill.uuid :refer [new-id]]
            [rill.message :as m]
            [studyflow.learning.course.events :as course]
            [studyflow.learning.entry-quiz.events :as entry-quiz]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.learning.tracking.events :as tracking]
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

(def model-teacher-also-teaches-1b
  (load-model model-with-fred-barney-and-wilma-in-other-class
              (teacher/class-assigned "teacher" "boulder" "1B")))

(def model-fred-moved-to-vegas
  (load-model model-teacher-also-teaches-1b
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
             (students-by-class model-with-fred-and-teacher teacher))))
    (testing "two students in the same class"
      (is (= 1 (count (classes model-with-fred-and-barney-in-same-class teacher))))
      (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}}
             (students-by-class model-with-fred-and-barney-in-same-class teacher))))
    (testing "three students; two in one, the other in another class"
      (is (= 1 (count (classes model-with-fred-barney-and-wilma-in-other-class teacher))))
      (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}}
             (students-by-class model-with-fred-barney-and-wilma-in-other-class teacher))))
    (let [teacher (first (vals (:teachers model-teacher-also-teaches-1b)))]
      (testing "three students; two in one, the other in another class all same teacher"
        (is (= 2 (count (classes model-teacher-also-teaches-1b teacher))))
        (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}
                "bedrock|boulder|1B" #{"Wilma Flintstone"}}
               (students-by-class model-teacher-also-teaches-1b teacher))))
      (testing "three students; two different departments"
        (is (= 2 (count (classes model-fred-moved-to-vegas teacher))))
        (is (= {"bedrock|boulder|1A" #{"Barney Rubble"}
                "bedrock|boulder|1B" #{"Wilma Flintstone"}}
               (students-by-class model-fred-moved-to-vegas teacher)))))))

(def course {:chapters [{:id "chapter-1"
                         :remedial true
                         :sections [{:id "section-1"
                                     :meijerink-criteria #{"A" "B"}
                                     :domains #{"Getallen" "Verhoudingen"}}
                                    {:id "section-2"
                                     :meijerink-criteria #{"B"}
                                     :domains #{"Meetkunde"}}]}
                        {:id "chapter-2"
                         :sections [{:id "section-3"
                                     :meijerink-criteria #{"C"}
                                     :domains #{"Meetkunde" "Verbanden"}}]}]})

(def model-with-fred-barney-and-course
  (load-model model-with-fred-and-barney-in-same-class
              (course/published "course" course)))

(defn- completion-for-class [model teacher]
  (let [class (->> teacher
                   (classes model)
                   first)
        students (->> (students-for-class model class)
                      (map (partial decorate-student-completion model)))]
    (:completion (decorate-class-completion model students class))))

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
        (is (not (seq (completion-for-class model teacher))))))
    (testing "one finished section without course material"
      (let [model (load-model model-with-fred-and-department-and-class
                              (section-test/finished "section-1" "fred"))]
        (is (not (seq (completion-for-class model teacher))))))
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
               (completion-for-class model teacher)))))
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
               (completion-for-class model teacher)))))))

(deftest chapter-list-test
  (let [model model-with-fred-barney-and-course
        teacher (first (vals (:teachers model)))
        class (first (classes model teacher))
        section-counts (fn [model] (get-in (chapter-list model class "chapter-1" "section-1")
                                           [:sections-total-status "chapter-1" "section-1"]))]
    (is (= 2 (:unstarted (section-counts model))))
    (is (= ["Barney Rubble" "Fred Flintstone"]
           (map :full-name (get-in (section-counts model) [:student-list :unstarted]))))
    (testing "fred starts a test"
      (let [model (load-model model (section-test/created "section-1" "fred" "course"))]
        (is (= 1 (:in-progress (section-counts model))))
        (is (= ["Fred Flintstone"]
               (map :full-name (get-in (section-counts model) [:student-list :in-progress]))))))
    (testing "fred gets stuck"
      (let [model (load-model model (section-test/stuck "section-1" "fred"))]
        (is (= 1 (:stuck (section-counts model))))
        (is (= ["Fred Flintstone"]
               (map :full-name (get-in (section-counts model) [:student-list :stuck]))))
        (testing "fred gets unstuck"
          (let [model (load-model model (section-test/unstuck "section-1" "fred"))]
            (is (= 1 (:in-progress (section-counts model))))
            (is (= ["Fred Flintstone"]
                   (map :full-name (get-in (section-counts model) [:student-list :in-progress]))))))))
    (testing "fred finishes a test"
      (let [model (load-model model (section-test/finished "section-1" "fred"))]
        (is (= 1 (:finished (section-counts model))))
        (is (= ["Fred Flintstone"]
               (map :full-name (get-in (section-counts model) [:student-list :finished]))))))))

(deftest test-time-spent
  (let [e0 (-> (student/created "fred" "Fred Flintstone")
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 0 0 0))))
        e11 (-> (section-test/question-assigned "section-1" "fred" "question-1" 3)
                (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 2 0 0))))
        e12 (-> (section-test/question-answered-correctly "section-1" "fred" "question-1" {})
                (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 3 0 0))))
        e13 (-> (section-test/question-assigned "section-1" "fred" "question-2" 3)
                (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 5 0 0))))
        model1 (load-model {} e0 e11 e12 e13)]
    (is (= (* (+ 3 5) 60)
           (get-in model1 [:students "fred" :section-time-spent "section-1" :total-secs])))
    (let [e21 (-> (section-test/question-assigned "section-2" "fred" "question-1" 3)
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 6 0 0))))
          e22 (-> (section-test/question-answered-incorrectly "section-2" "fred" "question-1" {})
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 12 0 0))))
          e23 (-> (section-test/answer-revealed "section-2" "fred" "question-1" 3)
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 13 0 0))))
          model2 (load-model model1 e21 e22 e23)]
      (is (= (* (+ 5 1 5) 60)
             (get-in model2 [:students "fred" :section-time-spent "section-2" :total-secs]))))
    (let [e21 (-> (section-test/question-assigned "section-2" "fred" "question-1" 3)
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 6 0 0))))
          e22 (-> (section-test/question-answered-incorrectly "section-2" "fred" "question-1" {})
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 12 0 0))))
          e23 (-> (section-test/answer-revealed "section-2" "fred" "question-1" 3)
                  (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 13 0 0))))
          model2 (load-model model1 e21 e22 e23)]
      (is (= (* (+ 5 1 5) 60)
             (get-in model2 [:students "fred" :section-time-spent "section-2" :total-secs]))))))

(deftest test-time-spent-with-end-time
  (let [e0 (-> (student/created "fred" "Fred Flintstone")
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 14 0 0 0))))
        e1 (-> (section-test/question-assigned "section-3" "fred" "question-1" 3)
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 15 8 0 0))))
        e2 (-> (section-test/question-answered-incorrectly "section-3" "fred" "question-1" {})
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 15 9 0 0))))
        e3 (-> (section-test/answer-revealed "section-3" "fred" "question-1" 3)
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 15 11 0 0))))
        e4 (-> (tracking/dashboard-navigated "fred")
               (assoc ::m/timestamp (ct/to-date (t/date-time 2014 9 19 15 12 0 0))))
        model (load-model {} e0 e1 e2 e3 e4)]
    (is (= (* (+ 1 2 1) 60)
           (get-in model [:students "fred" :section-time-spent "section-3" :total-secs])))))
