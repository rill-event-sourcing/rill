(ns rekenmachien.program-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest]])
  (:require [cemerick.cljs.test :as _]
            [rekenmachien.program :as t]))

(deftest render-result
  (are [value result] (= result (t/render-result value))
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
