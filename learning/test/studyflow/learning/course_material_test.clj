(ns studyflow.learning.course-material-test
  (:require [studyflow.learning.course-material :as material]
            [clojure.java.io :as io]
            [clojure.test :refer [is deftest testing]]
            [rill.uuid :refer [new-id]]
            [cheshire.core :as json]
            [studyflow.json-tools :refer [key-from-json]]))

(defn read-example-json
  []
  (json/parse-string (slurp (io/resource "dev/material.json")) key-from-json))

(deftest parsing-test
  (testing "parsing example json"
    (is (= (:name (material/parse-course-material (read-example-json)))
           "Counting")))

  (testing "throws exceptions when not valid"
    (is (thrown? RuntimeException (material/parse-course-material {:id "invalid" :name "Counting"})))))

(defn read-test-json []
  (json/parse-string (slurp (io/resource "dev/20140805-staging-material.json")) key-from-json))

(deftest test-question-text-to-html
  (let [json (read-test-json)
        test-q-title (get-in (material/parse-course-material (read-test-json))
                             [:chapters 0 :sections 0 :title])
        test-q (-> (get-in (material/parse-course-material (read-test-json))
                           [:chapters 0 :sections 0 :questions])
                   first)]
    (is (= test-q-title
           "TESTABLE QUESTIONS"))
    (is (= (:text test-q)
           "some prefix <p class=\"some-class\">Multiple choice B: _INPUT_1_ </p> text <p>123 into _INPUT_2_ </p>\r\n\r\n<p> 456 into _INPUT_4_ </p> some stuff after"))
    (is (= (:tag-tree test-q)
           '{:tag :div,
             :attrs nil,
             :content
             ("some prefix "
              {:tag :p,
               :attrs {:class "some-class"},
               :content
               ("Multiple choice B: "
                {:tag :input, :attrs {:name "_INPUT_1_"}, :content nil}
                " ")}
              " text "
              {:tag :p,
               :attrs nil,
               :content
               ("123 into "
                {:tag :input, :attrs {:name "_INPUT_2_"}, :content nil}
                " ")}
              "\n\n"
              {:tag :p,
               :attrs nil,
               :content
               (" 456 into "
                {:tag :input, :attrs {:name "_INPUT_4_"}, :content nil}
                " ")}
              " some stuff after")}))))
