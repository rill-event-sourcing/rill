(ns rekenmachien.math-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest testing]])
  (:require [cemerick.cljs.test :as _]
            [rekenmachien.math :as t :refer [Frac]]))

(deftest fraction
  (let [f #(Frac. %1 %2)]
    (is (instance? Frac (Frac. 1 2)))
    (testing "greatest common factor"
      (is (= 2 (.gcf (f 2 4))))
      (is (= 1 (.gcf (f 5 6)))))
    (testing "add"
      (is (= (f 3 2) (.add (f 1 2) 1)))
      (is (= (f 1 2) (.add (f -1 2) 1)))
      (is (= (f 3 4) (.add (f 1 2) (f 1 4))))
      (is (= (f 1 4) (.add (f 1 2) (f -1 4))))
      (is (= 1.6 (.add (f 1 2) 1.1))))
    (testing "subtract"
      (is (= (f -1 2) (.sub (f 1 2) 1)))
      (is (= (f -3 2) (.sub (f -1 2) 1)))
      (is (= (f 1 4) (.sub (f 1 2) (f 1 4))))
      (is (= (f 3 4) (.sub (f 1 2) (f -1 4))))
      (is (= -1 (.sub (f 1 2) 1.5))))
    (testing "multiplication"
      (is (= (f 3 2) (.mul (f 1 2) 3)))
      (is (= (f -3 2) (.mul (f -1 2) 3)))
      (is (= (f 1 8) (.mul (f 1 2) (f 1 4))))
      (is (= (f -1 8) (.mul (f 1 2) (f -1 4))))
      (is (= 0.75 (.mul (f 1 2) 1.5))))
    (testing "division"
      (is (= (f 1 6) (.div (f 1 2) 3)))
      (is (= (f -1 6) (.div (f -1 2) 3)))
      (is (= (f 2 1) (.div (f 1 2) (f 1 4))))
      (is (= (f -2 1) (.div (f 1 2) (f -1 4))))
      (is (= "0.3333" (.toFixed (.div (f 1 2) 1.5) 4))))
    (testing "exponent"
      (is (= (f 1 4) (.pow (f 1 2) 2)))
      (is (= (f 4 1) (.pow (f 1 2) -2)))
      (is (= (f 1 4) (.pow (f 1 2) (f 2 1))))
      (is (= "0.7071" (.toFixed (.pow (f 1 2) (f 1 2)) 4)))
      (is (= "0.7071" (.toFixed (.pow (f 1 2) 0.5) 4))))))
