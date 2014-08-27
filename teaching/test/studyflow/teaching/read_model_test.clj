(ns studyflow.teaching.read-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [studyflow.teaching.read-model :refer :all]
            [rill.uuid :refer [new-id]]
            [studyflow.learning.course.events :as course]
            [studyflow.learning.section-test.events :as section-test]
            [studyflow.school-administration.department.events :as department]
            [studyflow.school-administration.student.events :as student]
            [studyflow.teaching.read-model.event-handler :refer [handle-event]]))

(defn load-model [model & events] (reduce handle-event model events))

(deftest test-read-model
  (let [model {}
        students-by-class (fn [model]
                            (->> (classes model)
                                 (map #(vector (:id %) (->> (students-for-class model %) (map :full-name) set)))
                                 (into {})))]
    (testing "no students"
      (is (empty? (classes model))))
    (let [model (load-model model (student/created "fred" "Fred Flintstone"))]
      (testing "one student without departments"
        (is (empty? (classes model))))
      (let [model (load-model model
                              (department/created "boulder" "bedrock" "Boulder road")
                              (student/department-changed "fred" "boulder"))]
        (testing "one student with departments without classes"
          (is (empty? (classes model))))
        (let [model (load-model model
                                (department/created "boulder" "bedrock" "Boulder road")
                                (student/class-assigned "fred" "boulder" "1A"))]
          (testing "one students with departments with classes"
            (is (= 1 (count (classes model))))
            (is (= {"bedrock|boulder|1A" #{"Fred Flintstone"}}
                   (students-by-class model))))
          (let [model (load-model model
                                  (student/created "barney" "Barney Rubble")
                                  (student/department-changed "barney" "boulder")
                                  (student/class-assigned "barney" "boulder" "1A"))]
            (testing "two students in the same class"
              (is (= 1 (count (classes model))))
              (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}}
                     (students-by-class model))))
            (let [model (load-model model
                                    (student/created "wilma" "Wilma Flintstone")
                                    (student/department-changed "wilma" "boulder")
                                    (student/class-assigned "wilma" "boulder" "1B"))]
              (testing "three students; two in one, the other in another class"
                (is (= 2 (count (classes model))))
                (is (= {"bedrock|boulder|1A" #{"Fred Flintstone" "Barney Rubble"}
                        "bedrock|boulder|1B" #{"Wilma Flintstone"}}
                       (students-by-class model))))
              (let [model (load-model model
                                      (department/created "vegas" "bedrock" "Rock Vegas")
                                      (student/department-changed "fred" "vegas"))]
                (testing "three students; two different departments"
                  (is (= 3 (count (classes model))))
                  (is (= {"bedrock|boulder|1A" #{"Barney Rubble"}
                          "bedrock|boulder|1B" #{"Wilma Flintstone"}
                          "bedrock|vegas|1A" #{"Fred Flintstone"}}
                         (students-by-class model))))))
            (testing "class completion"
              (is (= {:finished 0, :total 0} (:total-completion (first (classes model))))))
            (let [model (load-model model  (section-test/finished "section-1" "fred"))]
              (testing "one finished section without course material"
                (is (= {:finished 0, :total 0} (:total-completion (first (classes model))))))
              (let [model (load-model model (course/published "course-1"
                                                              {:chapters [{:sections [{:id "section-1"}
                                                                                      {:id "section-2"}]}
                                                                          {:sections [{:id "section-3"}]}]}))]
                (testing "one finished section by one student in three section course material"
                  (is (= {:finished 1, :total 6} (:total-completion (first (classes model))))))))))))))
