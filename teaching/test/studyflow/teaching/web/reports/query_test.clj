(ns studyflow.teaching.web.reports.query-test
  (:require [clojure.test :refer [deftest is testing]]
            [net.cgrand.enlive-html :as enlive]
            [ring.mock.request :refer [request]]
            [studyflow.teaching.web.reports.query :as t]))

;; TODO move to common place
(defn query-html [data pattern]
  (seq (enlive/select
        (if (string? data)
          (enlive/html-snippet data)
          data)
        pattern)))

(defn query-html-content [data pattern]
  (apply str (mapcat :content (query-html data pattern))))

(deftest render-completion
  (testing "without data"
    (let [body (t/render-completion nil nil nil nil nil nil nil nil)]
      (is (= "Overzicht - Studyflow"
             (query-html-content body [[:html] [:head] [:title]])))))
  (testing "with data"
    (let [class {:id "c"
                 :full-name "C1"
                 :completion {"A" {:all {:finished 0, :total 10}
                                   "bar" {:finished 3, :total 37}}}}
          body (t/render-completion class
                                    "A"
                                    [{:id "fred",
                                      :full-name "Fred Flintstone",
                                      :completion {"A" {:all {:finished 1, :total 10}
                                                        "foo" {:finished 2, :total 17}}}}]
                                    [class]
                                    #{"A" "B"}
                                    #{"foo" "bar"}
                                    {:class-id "c", :meijerink "A"}
                                    nil)]
      (is (= "Overzicht voor \"C1\" - Studyflow"
             (query-html-content body [[:html] [:head] [:title]])))
      (is (= "Fred Flintstone"
             (query-html-content body [[:table.students]
                                       [:tr.student#student-fred]
                                       [:td.full-name]])))
      (is (= "10%"
             (query-html-content body [[:table.students]
                                       [:tr.student#student-fred]
                                       [:td.completion.number.all]
                                       [:span (enlive/attr= :title "1/10")]])))
      (is (= "12%"
             (query-html-content body [[:table.students]
                                       [:tr.student#student-fred]
                                       [:td.completion.number.foo]
                                       [:span (enlive/attr= :title "2/17")]])))
      (is (= "0%"
             (query-html-content body [[:table.students]
                                       [:tfoot]
                                       [:td.average.number.all]
                                       [:span (enlive/attr= :title "0/10")]])))
      (is (= "8%"
             (query-html-content body [[:table.students]
                                       [:tfoot]
                                       [:td.average.number.bar]
                                       [:span (enlive/attr= :title "3/37")]]))))))

(deftest app
  (testing "GET /reports"
    (let [resp (t/app (request :get "/reports/"))]
      (is (= 303 (:status resp)))
      (is (= "/reports/completion" (get-in resp [:headers "Location"])))))
  (testing "GET /reports/completion"
    (let [resp (t/app (-> (request :get "/reports/completion")
                          (assoc :read-model
                            {:teachers {"t" {:classes #{{:department-id "d"
                                                         :class-name "1"}
                                                        {:department-id "d"
                                                         :class-name "2"}}}}
                             :students {}})))]
      (is (= 200 (:status resp)))
      (is (= "text/html; charset=utf-8" (get-in resp [:headers "Content-Type"]))))))
