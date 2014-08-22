(ns rill.timestamp-test
  (:require [rill.timestamp :refer [now]]
            [clojure.test :refer :all]))

(deftest test-timestamp
  (testing "Resolution"
    (is (now))
    (is (not= (now)
              (do
                (Thread/sleep 2) (now))))))

