(ns rekenmachien.core-test
  (:require [cemerick.cljs.test :refer [run-all-tests]]
            [rekenmachien.parser-test :as parser-test]
            [rekenmachien.program-test :as program-test]))

(defn ^:export run []
  (let [output (atom "")]
    (set-print-fn! (fn [& args] (swap! output #(str % "\n" (apply str args)))))
    (run-all-tests)
    (set! (.-textContent (.getElementById js/document "output")) @output)))
