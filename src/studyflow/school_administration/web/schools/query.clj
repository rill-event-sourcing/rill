(ns studyflow.school-administration.web.schools.query
  (:require [compojure.core :refer [GET defroutes]]
            [clojure.string :as string]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [rill.uuid :refer [uuid]]
            [studyflow.school-administration.read-model :as read-model]
            [studyflow.school-administration.web.html-util :refer [*current-nav-uri*
                                                                   anti-forgery-field
                                                                   field-errors
                                                                   layout]]))

(defn- cancel-button []
  [:a.button.cancel {:href "/list-schools"} "Cancel"])

(defn render-list [schools options]
  (layout
   (merge {:title "School list"} options)

   [:div.actions
    [:a.button.primary {:href "/new-school"} "New school"]]

   (if (seq schools)
     [:table
      [:thead
       [:tr
        [:th.name "Name"]
        [:th.licenses-sold.number "Licenses sold"]
        [:th.students.number "Students"]
        [:th.actions]]]
      [:tbody
       (map (fn [{:keys [id name licenses-sold student-count]}]
              [:tr.school
               [:td.name (h name)]
               [:td.licenses-sold.number licenses-sold]
               [:td.students.number student-count]
               [:td.actions
                [:a.button.edit {:href (str "/edit-school/" id)} "Edit"]]])
            schools)]]
     [:div.no-records "No schools added yet."])))

(defn render-new [{:keys [name brin]} {:keys [errors] :as options}]
  (layout
   (merge {:title "New school"} options)

   (form/form-to
    [:post "/create-school"]

    [:fieldset
     (anti-forgery-field)
     [:div.field
      (form/label "name" "Name")
      (form/text-field {:placeholder "ex. OSG Amsterdam"} "name" name)
      (field-errors (:name errors))]
     [:div.field
      (form/label "brin" "BRIN")
      (form/text-field {:placeholder "ex. 01ET00"} "brin" brin)
      (field-errors (:brin errors))]
     [:div.actions
      [:button.primary {:type "submit"} "Add school"]
      (cancel-button)]])))

(defn teachers-table
  [teachers]
  [:table
   [:caption "Teachers"]
   [:thead
    [:tr [:th.name "Name"] [:th.email "E-mail"] [:th.department "Department"] [:th.department "Classes"] [:th.actions]]]
   (into [:tbody] (map (fn [{:keys [full-name email department class-names id]}]
                         [:tr
                          [:td (h full-name)]
                          [:td (h email)]
                          [:td (h (:name department))]
                          [:td (h (string/join #", " class-names))]
                          [:td.actions
                           [:a.button.edit {:href (str "/edit-teacher/" id)} "Edit"]]])
                       teachers))])
(defn students-table
  [students]
  [:table
   [:caption "Students"]
   [:thead
    [:tr [:th.name "Name"] [:th.email "E-mail"] [:th.deparmetn "Department"] [:th.department "Class"]  [:th.actions]]]
   (into [:tbody] (map (fn [{:keys [full-name email class-name department id]}]
                         [:tr
                          [:td (h full-name)]
                          [:td (h email)]
                          [:td (h (:name department))]
                          [:td (h class-name)]
                          [:td.actions
                           [:a.button.edit {:href (str "/edit-student/" id)} "Edit"]]])
                       students))])

(defn render-edit [school teachers students post-params {:keys [errors] :as options}]
  (let [{:keys [id version name brin]} (merge school post-params)
        departments (:departments school)
        {original-name :name} school]
    (layout
     (merge {:title (str "Edit school: " original-name)} options)

     (form/form-to
      [:post "/change-school-name"]

      [:fieldset
       (anti-forgery-field)
       (form/hidden-field "school-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "name" "Name")
        (form/text-field {:placeholder "ex. OSG Amsterdam"} "name" name)
        (field-errors (:name errors))]
       [:div.field
        (form/label "brin" "BRIN")
        (form/text-field {:disabled true} "brin" brin)]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]])

     [:fieldset
      [:legend "Departments"]

      [:div.actions
       [:a.button.primary {:href (str "/new-department/" id)} "New department"]]

      (if (seq departments)
        [:table
         [:thead
          [:tr
           [:th.name "Name"]
           [:th.status "Status"]
           [:th.licenses-sold.number "Licenses sold"]
           [:th.students.number "Students"]
           [:th.actions]]]
         [:tbody
          (map (fn [{department-id :id :keys [name student-count sales-data]}]
                 [:tr.department
                  [:td.name (h name)]
                  [:td.status (h (:status sales-data))]
                  [:td.licenses-sold.number (:licenses-sold sales-data)]
                  [:td.students.number student-count]
                  [:td.actions
                   [:a.button.edit {:href (str "/edit-department/" id "/" department-id)} "Edit"]]])
               departments)]]
        [:div.no-records "No departments added yet."])]

     (teachers-table teachers)
     (students-table students))))


(defroutes app
  (GET "/list-schools" {:keys [read-model flash]}
       (render-list (read-model/list-schools read-model) {:flash flash}))
  (GET "/new-school" {:keys [flash]}
       (render-new (:post-params flash) flash))
  (GET "/edit-school/:id" {:keys [read-model]
                           {:keys [id]} :params
                           {:keys [post-params errors warning] :as flash} :flash}
       (let [school (read-model/get-school read-model (uuid id))
             department-ids  (set (map :id (:departments school)))]
         (render-edit school
                      (read-model/list-teachers read-model department-ids)
                      (read-model/list-students read-model department-ids)
                      post-params flash))))

(def queries
  (fn [req]
    (binding [*current-nav-uri* "/list-schools"]
      (app req))))
