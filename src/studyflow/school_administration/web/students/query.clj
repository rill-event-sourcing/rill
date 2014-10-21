(ns studyflow.school-administration.web.students.query
  (:require [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [rill.uuid :refer [uuid]]
            [studyflow.school-administration.read-model :as read-model]
            [studyflow.school-administration.web.html-util :refer [*current-nav-uri*
                                                                   anti-forgery-field
                                                                   field-errors
                                                                   layout]]))

(defn- cancel-button []
  [:a.button.cancel {:href "/list-students"} "Cancel"])

(defn render-list [students options]
  (layout
   (merge {:title "Student list"} options)

   [:div.actions
    [:a.button.primary {:href "/new-student"} "New student"]]

   (if (seq students)
     [:table
      [:thead
       [:tr
        [:th.name "Name"]
        [:th.email "E-mail"]
        [:th.department "School"]
        [:th.department "Department"]
        [:th.department "Class"]
        [:th.actions]]]
      [:tbody
       (map (fn [{:keys [id full-name email class-name]
                  {department-name :name} :department
                  {school-name :name} :school}]
              [:tr.student
               [:td.name (h full-name)]
               [:td.email (h email)]
               [:td.school (h school-name)]
               [:td.department (h department-name)]
               [:td.class-name (h class-name)]
               [:td.actions
                [:a.button.edit {:href (str "/edit-student/" id)} "Edit"]]])
            students)]]
     [:div.no-records "No students added yet."])))

(defn render-new [{:keys [full-name]} {:keys [errors] :as options}]
  (layout
   (merge {:title "New student"} options)

   (form/form-to
    [:post "/create-student"]

    [:fieldset
     (anti-forgery-field)
     [:div.field
      (form/label "full-name" "Name")
      (form/text-field {:placeholder "ex. Peter de Vries"} "full-name" full-name)
      (field-errors (:full-name errors))]
     [:div.actions
      [:button.primary {:type "submit"} "Add student"]
      (cancel-button)]])))

(defn render-edit [student departments post-params {:keys [errors] :as options}]
  (let [{:keys [id version full-name email department class-name]} (merge student post-params)
        {original-email :email, original-full-name :full-name} student]
    (layout
     (merge {:title (str "Edit student: " original-full-name)} options)

     (form/form-to
      [:post "/change-student-name"]

      [:fieldset
       [:legend "Personal"]
       (anti-forgery-field)
       (form/hidden-field "student-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "full-name" "Name")
        (form/text-field {:placeholder "ex. Peter de Vries"} "full-name" full-name)
        (field-errors (:full-name errors))]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]])

     (form/form-to
      [:post "/change-student-department"]

      [:fieldset
       [:legend "School"]
       (anti-forgery-field)
       (form/hidden-field "student-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "department-id" "Department")
        (form/drop-down "department-id"
                        (into [[]]
                              (sort-by first
                                       (map #(vector (str (:name (:school %)) " - " (:name %))
                                                     (:id %))
                                            departments)))
                        (:id department))

        (field-errors (:department-id errors))]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]])

     (when department
       (form/form-to
        [:post "/change-student-class"]

        [:fieldset
         [:legend "Class"]
         (anti-forgery-field)
         (form/hidden-field "student-id" id)
         (form/hidden-field "department-id" (:id department))
         (form/hidden-field "expected-version" version)
         [:div.field
          (form/label "class-name" "Class")
          (form/text-field {:placeholder "ex. HAVO 3f"} "class-name" class-name)
          (field-errors (:class-name errors))]
         [:div.actions
          [:button.primary {:type "submit"} "Update"]
          (cancel-button)]]))

     (form/form-to
      [:post "/change-student-credentials"]

      [:fieldset
       [:legend "Credentials"]
       (anti-forgery-field)
       (form/hidden-field "student-id" id)
       (form/hidden-field "expected-version" version)
       (form/hidden-field "original-email" original-email)
       [:div.field
        (form/label "email" "Email")
        (form/email-field {:placeholder "ex. somebody@example.com", :autocomplete "off"} "email" email)
        (field-errors (:email errors))]
       [:div.field
        (form/label "password" "Password")
        (form/password-field {:autocomplete "off"} "password")]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]]))))

(defroutes app
  (GET "/list-students" {:keys [read-model flash]}
       (render-list (read-model/list-students read-model) {:flash flash}))
  (GET "/new-student" {:keys [flash]}
       (render-new (:post-params flash) flash))
  (GET "/edit-student/:id" {:keys [read-model]
                            {:keys [id]} :params
                            {:keys [post-params errors warning] :as flash} :flash}
       (let [student (read-model/get-student read-model (uuid id))
             departments (read-model/list-departments read-model)]
         (render-edit student departments post-params flash))))

(def queries
  (fn [req]
    (binding [*current-nav-uri* "/list-students"]
      (app req))))
