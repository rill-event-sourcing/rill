(ns rekenmachien.core-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest run-tests]])
  (:require [cemerick.cljs.test :as _]))

(enable-console-print!)

(defn ^:export run []
  (prn-str (run-tests 'rekenmachien.parser-test)))
