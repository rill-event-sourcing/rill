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

     [:h1#page-title "Klas Overzicht"]
     (when class
       [:div
        (drop-list-meijerink class meijerink-criteria report-name scope)
        [:span#clarification (condp = scope
                               "1F-RT" "Dit rapport gaat alleen over de 1F-RT hoofdstukken (hoofdstuk 1 t/m 6)"
                               "2F" "Dit rapport gaat alleen over de 2F hoofdstukken (7 t/m 26). Werk aan 1F-RT hoofdstukken wordt hier niet getoond."
                               "3F" "Dit rapport gaat alleen over de 3F hoofdstukken (7 t/m 29). Werk aan 1F-RT hoofdstukken wordt hier niet getoond."
                               "")]])
     (when students
       [:div
        [:table.students
         [:thead
          [:th.full-name "Leerling"]
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
          [:td.average "Gemiddelde"]
          [:td.average.number.time-spent
           (time-spent-html (get-in class [:time-spent scope]))]
          (map (fn [domain]
                 [:td.average.number {:class (classerize domain)}
                  (completion-html (get-in class [:completion scope domain]))])
               (into [:all] domains))]]
        [:a {:href (str "/reports/" (:class-id params) "/completion/export") :target "_blank" :class "btn small gray export-btn"} "Exporteren naar Excel"]]))))

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
    (binding [*current-report-name* "completion"]
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
       (redirect-after-post (str "/reports/" (:class-id params) "/1F-RT/completion")))

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
