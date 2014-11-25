(ns rekenmachien.core-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest]])
  (:require [cemerick.cljs.test :refer [run-all-tests]]
            [rekenmachien.core :as t]
            [rekenmachien.math-test :as math-test]
            [rekenmachien.parser-test :as parser-test]
            [rekenmachien.program-test :as program-test]))

(deftest decimal->str
  (are [value result] (= result (t/decimal->str value))
       0             , "0"
       1             , "1"
       1.1           , "1,1"
       0.001         , "0,001"
       0.0000000001  , "1×10^-10"
       0.00000000012 , "1,2×10^-10"
       (/ 1 3)       , "0,3333333333"
       12.00000003   , "12,00000003"
       12.000000003  , "12"
       120.0000003   , "120,0000003"
       120.00000003  , "120"
       10000000000   , "1×10^10"
       10000000001   , "1×10^10"))

(defn ^:export run []
  (let [output (atom "")]
    (set-print-fn! (fn [& args] (swap! output #(str % "\n" (apply str args)))))
    (run-all-tests)
    (set! (.-textContent (.getElementById js/document "output")) @output)))
