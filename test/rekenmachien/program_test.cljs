(ns rekenmachien.program-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest]])
  (:require [cemerick.cljs.test :as _]
            [rekenmachien.program :as t]))

(deftest render-result
  (are [value result] (= result (t/render-result value))
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

(deftest run
  (are [tokens result] (= result (t/run {:tokens tokens}))
       [1 :add 1]           , "2"
       [1 0 :add 1]         , "11"
       [:dot 2 :add :dot 4] , "0,6"
       [1 :add 2 :mul 3]    , "7"
       [:sin 0]             , "0"
       [:sin 9 0]           , "1"
       [:sin 8 0 :add 1 0]  , "1"
       [:sin 4 5 0]         , "1"
       [:asin 1]            , "90"
       [:cos 0]             , "1"
       [:cos 9 0]           , "0"
       [:cos :neg 9 0]      , "0"
       [:cos 4 5 0]         , "0"
       [:tan 0]             , "0"
       [:tan 90]            , "MATH ERROR"))
