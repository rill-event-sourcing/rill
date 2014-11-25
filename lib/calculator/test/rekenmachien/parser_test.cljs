(ns rekenmachien.parser-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest]])
  (:require [cemerick.cljs.test :as _]
            [rekenmachien.parser :as t]))

(deftest decimals
  (are [tokens result] (= result (t/decimals tokens))
       [1]           , [1]
       [1 2]         , [12]
       [1 :x 2]      , [1 :x 2]
       [:x 1 2]      , [:x 12]
       [1 2 :x 3 4]  , [12 :x 34]
       [1 2 :x]      , [12 :x]
       [1 :dot 2 :x] , [1.2 :x]))

(deftest blocks
  (are [tokens result] (= result (t/blocks tokens))
       [1 :x 2 :x 3]                     , [1 :x 2 :x 3]
       [:sin 1 :cos 2 :close 3 :close 4] , [[:sin 1 [:cos 2] 3] 4]))

(deftest infix-walker
  (are [tokens result] (= result (t/infix-walker tokens :op))
       [1 :op 2]         , [[:op 1 2]]
       [:x 1 :op :y]     , [:x [:op 1 :y]]
       [:op 1 2 :op 3 4] , [:op 1 [:op 2 3] 4]))

(deftest neg-oper
  (are [tokens result] (= result (t/neg-oper tokens))
       [:neg 1]        , [[:neg 1]]
       [1 :neg 2]      , [1 [:neg 2]]
       [1 :add :neg 2] , [1 :add [:neg 2]]
       [:neg 1 :add 2] , [[:neg 1] :add 2]))

(deftest squash-negs
  (are [tokens result] (= result (t/squash-negs tokens))
       [:neg :neg 1] , [1]))

(deftest dangling-subs
  (are [tokens result] (= result (t/dangling-subs tokens))
       [1 :add :sub 2]      , [1 :add :neg 2]
       [1 :div :sub 2]      , [1 :div :neg 2]
       [1 :add :sub :sub 2] , [1 :add :neg :neg 2]))

(deftest special-powers
  (are [tokens result] (= result (t/special-powers tokens))
       [1 :x1]     , [1 :pow -1]
       [1 :x2]     , [1 :pow 2]
       [1 :x1 :x2] , [1 :pow -1 :pow 2]
       [1 :x2 :x1] , [1 :pow 2 :pow -1]))

(deftest negs
  (are [tokens result] (= result (t/negs tokens))
       [:neg 1 :add :neg 2] , [[:neg 1] :add [:neg 2]]))

(deftest parse
  (are [tokens result] (= result (t/parse tokens))
       [1 :add 2]                              , [:add 1 2]
       [:add 2]                                , [:add 0 2]
       [:neg 5 :x2]                            , [:neg [:pow 5 2]]
       [5 :pow :neg 2]                         , [:pow 5 [:neg 2]]
       [:neg 5 :pow :neg 2]                    , [:neg [:pow 5 [:neg 2]]]
       [:neg 1 :add :neg 2]                    , [:add [:neg 1] [:neg 2]]
       [1 :add :neg 2 :mul 3]                  , [:add 1 [:mul [:neg 2] 3]]
       [1 :add 2 :add 3 :add 4]                , [:add [:add [:add 1 2] 3] 4]
       [1 :add 2 :mul 3 :add 4]                , [:add [:add 1 [:mul 2 3]] 4]
       [[:open 1 :add 2] :pow 3 :mul 4 :add 5] , [:add [:mul [:pow [:open [:add 1 2]] 3] 4] 5]
       [1 :add :sub 2]                         , [:add 1 [:neg 2]]
       [1 :add :sub :sub 2]                    , [:add 1 2]
       [1 :add :sub :neg 2]                    , [:add 1 2]
       [1 :add :neg :sub 2]                    , [:add 1 2]
       [2 :pow :neg 2]                         , [:pow 2 [:neg 2]]
       [:sin 1 :x2]                            , [:sin [:pow 1 2]]))
