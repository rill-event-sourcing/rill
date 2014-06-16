(ns studyflow.web.handler-tools-test
  (:require [studyflow.web.handler-tools :refer [combine-ring-handlers]]
            [clojure.test :refer [deftest is testing]]))

(deftest test-handler-tools
  (testing "combining handlers"
    (let [h1 :val1
          h2 :val2
          r  {:val1 1 :val2 2}]
      (is (= ((combine-ring-handlers h1 h2) r)
             1))
      (is (= ((combine-ring-handlers h2 h1) r)
             2))
      (is (nil? ((combine-ring-handlers h1 h2) {:val3 3}))))))
