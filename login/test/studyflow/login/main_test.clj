(ns studyflow.login.main-test
  (:require [clojure.test :refer :all]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as enlive]
            [studyflow.login.main :refer :all]))

(defn query-hiccup [data pattern]
  (enlive/select (enlive/html-snippet (hiccup/html data)) pattern))

(defn query-hiccup-content [data pattern]
  (apply str (:content (first (query-hiccup data pattern)))))

(deftest home-page
  (let [data (home 314)]
    (testing "Home page includes a welcome message."
      (is (= "welcome home" (query-hiccup-content data [:h2]))))))
