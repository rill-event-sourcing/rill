(ns rill.handler-test
  (:require [rill.handler :as handler :refer [aggregate-ids defaggregate-ids]]
            [rill.message :refer [defcommand]]
            [clojure.test :refer [deftest testing is]]))

(defcommand TestCommand1 [something my-id other-thing])
(defaggregate-ids TestCommand1 my-id)

(defcommand TestCommand2 [id-one something id-two other-thing])
(defaggregate-ids TestCommand2 id-one id-two)

(deftest aggregate-ids-test
  (is (= (aggregate-ids (->TestCommand1 12345 :a :b :c))
         [:b]))
  (is (= (aggregate-ids (->TestCommand2 12345 :a :b :c :d))
         [:a :c])))

