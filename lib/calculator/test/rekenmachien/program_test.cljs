(ns rekenmachien.program-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest]])
  (:require [cemerick.cljs.test :as _]
            [rekenmachien.math :refer [Frac]]
            [rekenmachien.program :as t]))

(deftest run
  (are [tokens result] (= result (t/run {:tokens tokens}))
       [1 :add 1]                  , 2
       [1 0 :add 1]                , 11
       [2 :add :dot 4]             , 2.4
       [1 :add 2 :mul 3]           , 7
       [1 :frac 2]                 , (Frac. 1 2)
       [1 :frac 2 :mul 3]          , (Frac. 3 2)
       [1 :frac 2 :div 3]          , (Frac. 1 6)
       [3 :div 1 :frac 2]          , (Frac. 6 1)
       [2 :frac 3 :frac 4]         , (Frac. 11 4)
       [2 :frac 3 :pow 2]          , (Frac. 4 9)
       [2 :frac 3 :x2]             , (Frac. 4 9)
       [2 :frac 3 :pow -1]         , (Frac. 3 2)
       [2 :frac 3 :pow :neg 1]     , (Frac. 3 2)
       [2 :frac 3 :x1]             , (Frac. 3 2)
       [5 :pow 2 :pow :sub 3]      , 0.000064
       [2 :pow :neg 2]             , 0.25
       [2 :sub :sub 2]             , 4
       [:sin 0]                    , 0
       [:sin 9 0]                  , 1
       [:sin 8 0 :add 1 0]         , 1
       [:sin 4 5 0]                , 1
       [:asin 1]                   , 90
       [:cos 0]                    , 1
       [:cos 9 0]                  , 0
       [:cos :neg 9 0]             , 0
       [:cos 4 5 0]                , 0
       [:tan 0]                    , 0
       [1 :div 0]                  , "MATH ERROR"
       [:tan 90]                   , "MATH ERROR"
       [:sqrt]                     , "SYNTAX ERROR"
       [1 :frac 2 :frac 3 :frac 4] , "SYNTAX ERROR"))
