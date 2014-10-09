(ns studyflow.teaching.web.reports.completion
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [studyflow.teaching.read-model :as read-model]
            [studyflow.teaching.web.reports.export :refer [render-export]]
            [studyflow.teaching.web.util :refer :all]
            [ring.util.response :refer [redirect-after-post]]))

(defn render-completion [class scope students classes meijerink-criteria domains params options]
  (let [meijerink-criteria (sort meijerink-criteria)
        domains (sort domains)
        report-name "completion"
        scope (if (str/blank? scope) nil scope)]
    (layout
     (merge {:title (if class
                      (str "Overzicht voor \"" (:full-name class) "\"")
                      "Overzicht")}
            options)

     (drop-list-classes classes scope report-name (:class-name class))
     (when class
       (drop-list-meijerink class meijerink-criteria report-name scope))

     (when students
       [:div
        [:table.students
         [:thead
          [:th.full-name]
          [:th.completion.number "Tijd"]
          [:th.completion.number "Totaal"]
          (map (fn [domain]
                 [:th.domain.number (h domain)])
               domains)]
         [:tbody
          (map (fn [student]
                 [:tr.student {:id (str "student-" (:id student))}
                  [:td.full-name
                   (h (:full-name student))]
                  [:td.completion.number.time-spent
                   (time-spent-html (get-in student [:time-spent scope]))]
                  (map (fn [domain]
                         [:td.completion.number {:class (classerize domain)}
                          (completion-html (get-in student [:completion scope domain]))])
                       (into [:all] domains))])
               (sort-by :full-name students))]
         [:tfoot
          [:th.average "Klassengemiddelde"]
          [:td.average.number.time-spent
           (time-spent-html (get-in class [:time-spent scope]))]
          (map (fn [domain]
                 [:td.average.number {:class (classerize domain)}
                  (completion-html (get-in class [:completion scope domain]))])
               (into [:all] domains))]]
        [:a {:href (str "/reports/" (:class-id params) "/completion/export") :target "_blank"} "Exporteren naar Excel"]]))))

(defn completion [read-model teacher redirect-urls params]
  (let [classes (read-model/classes read-model teacher)
        selected-meijerink (:meijerink params)
        meijerink-criteria (read-model/meijerink-criteria read-model)
        domains (read-model/domains read-model)
        class (some (fn [class]
                      (when (= (:class-id params) (:id class))
                        class)) classes)
        students (when class
                   (->> (read-model/students-for-class read-model class)
                        (map (comp (partial read-model/decorate-student-completion read-model)
                                   (partial read-model/decorate-student-time-spent read-model)))))
        class (if students
                (->> class
                     (read-model/decorate-class-completion read-model students)
                     (read-model/decorate-class-time-spent read-model students))
                class)
        options {:redirect-urls redirect-urls}]
    (binding [*current-nav-uri* "/reports/completion"]
      (render-completion class selected-meijerink students classes meijerink-criteria domains params options))))

(defroutes completion-routes
  (GET "/reports/"
       {}
       (redirect-after-post "/reports/completion"))

  (GET "/reports/completion"
       {:keys [read-model teacher redirect-urls]}
       (completion read-model teacher redirect-urls nil))

  (GET "/reports/:class-id/completion"
       {:keys [read-model teacher redirect-urls]
        params :params}
       (completion read-model teacher redirect-urls params))

  (GET "/reports/:class-id/completion/export"
       {:keys [read-model teacher]
        {:keys [class-id] :as params} :params}
       (let [classes (read-model/classes read-model teacher)
             domains (sort (read-model/domains read-model))
             meijerink-criteria (sort (read-model/meijerink-criteria read-model))
             class (some (fn [class]
                           (when (= class-id (:id class))
                             class)) classes)
             students (when class
                        (->> (read-model/students-for-class read-model class)
                             (map (comp (partial read-model/decorate-student-completion read-model)
                                        (partial read-model/decorate-student-time-spent read-model)))))
             class (if students
                     (->> class
                          (read-model/decorate-class-completion read-model students)
                          (read-model/decorate-class-time-spent read-model students))
                     class)]
         (render-export class students domains meijerink-criteria)))

  (GET "/reports/:class-id/:meijerink/completion"
       {:keys [read-model teacher redirect-urls]
        params :params}
       (completion read-model teacher redirect-urls params)))
