(ns rill.uuid-test
  (:require [rill.uuid :refer :all]
            [clojure.test :refer [deftest testing is]]))

(deftest uuid-test
  (is (= #uuid "df5323c9-1e2f-40d6-ab33-202b92768a55"
         (uuid "df5323c9-1e2f-40d6-ab33-202b92768a55")))
  (is (= #uuid "df5323c9-1e2f-40d6-ab33-202b92768a55"
         (uuid :df5323c9-1e2f-40d6-ab33-202b92768a55))))
