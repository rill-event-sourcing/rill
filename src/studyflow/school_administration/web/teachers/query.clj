(ns studyflow.school-administration.web.teachers.query
  (:require [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [rill.uuid :refer [uuid new-id]]
            [clojure.string :as string]
            [studyflow.school-administration.read-model :as read-model]
            [studyflow.school-administration.web.html-util :refer [*current-nav-uri*
                                                                   anti-forgery-field
                                                                   field-errors
                                                                   layout]]))

(defn department-field
  [department-id departments errors]
  [:div.field
   (form/label "department-id" "Department")
   (form/drop-down "department-id"
                   (into [[]]
                         (sort-by first
                                  (map #(vector (str (:name (:school %)) " - " (:name %))
                                                (:id %))
                                       departments)))
                   department-id)

   (field-errors (:department-id errors))])

(defn- cancel-button []
  [:a.button.cancel {:href "/list-teachers"} "Cancel"])

(defn render-list [teachers options]
  (layout
   (merge {:title "Teacher list"} options)

   [:div.actions
    [:a.button.primary {:href "/new-teacher"} "New teacher"]]

   (if (seq teachers)
     [:table
      [:thead
       [:tr
        [:th.name "Name"]
        [:th.email "E-mail"]
        [:th.department "School"]
        [:th.department "Department"]
        [:th.department "Classes"]
        [:th.actions]]]
      [:tbody
       (map (fn [{:keys [id full-name email class-names]
                  {department-name :name} :department
                  {school-name :name} :school}]
              [:tr.teacher
               [:td.name (h full-name)]
               [:td.email (h email)]
               [:td.school (h school-name)]
               [:td.department (h department-name)]
               [:td.class-names (h (string/join ", " (sort class-names)))]
               [:td.actions
                [:a.button.edit {:href (str "/edit-teacher/" id)} "Edit"]]])
            teachers)]]
     [:div.no-records "No teachers added yet."])))

(defn render-new [{:keys [full-name department-id]} departments {:keys [errors] :as options}]
  (layout
   (merge {:title "New teacher"} options)

   (form/form-to
    [:post "/create-teacher"]

    [:fieldset
     (anti-forgery-field)
     [:div.field
      (form/label "full-name" "Name")
      (form/text-field {:placeholder "ex. Peter de Vries"} "full-name" full-name)
      (field-errors (:full-name errors))]
     (department-field department-id departments errors)
     [:div.actions
      [:button.primary {:type "submit"} "Add teacher"]
      (cancel-button)]])))



(defn render-edit [teacher departments all-class-names post-params {:keys [errors] :as options}]
  (let [{:keys [id version full-name email department class-names]} (merge teacher post-params)
        {original-email :email, original-full-name :full-name} teacher]
    (layout
     (merge {:title (str "Edit teacher: " original-full-name)} options)

     (form/form-to
      [:post "/change-teacher-name"]

      [:fieldset
       [:legend "Personal"]
       (anti-forgery-field)
       (form/hidden-field "teacher-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "full-name" "Name")
        (form/text-field {:placeholder "ex. Peter de Vries"} "full-name" full-name)
        (field-errors (:full-name errors))]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]])

     (form/form-to
      [:post "/change-teacher-department"]

      [:fieldset
       [:legend "School"]
       (anti-forgery-field)
       (form/hidden-field "teacher-id" id)
       (form/hidden-field "expected-version" version)
       (department-field (:id department) departments errors)
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button)]])

     (when department
       (form/form-to
        [:post "/change-teacher-classes"]

        [:fieldset
         [:legend "Classes"]
         (anti-forgery-field)
         (form/hidden-field "teacher-id" id)
         (form/hidden-field "department-id" (:id department))
         (form/hidden-field "expected-version" version)
         (map #(let [id (str (new-id))]
                 [:div.field
                  [:label {:for id} (h %)]
                  (form/check-box {:id id} "class-names[]" (get class-names %) %)])
              (sort all-class-names))
         (field-errors (:class-names errors))
         [:div.actions
          [:button.primary {:type "submit"} "Update"]
          (cancel-button)]]))

     (form/form-to
      [:post "/change-teacher-credentials"]

      [:fieldset
       [:legend "Credentials"]
       (anti-forgery-field)
       (form/hidden-field "teacher-id" id)
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
  (GET "/list-teachers" {:keys [read-model flash]}
       (render-list (read-model/list-teachers read-model) {:flash flash}))
  (GET "/new-teacher" {:keys [flash read-model]}
       (render-new (:post-params flash) (read-model/list-departments read-model) flash))
  (GET "/edit-teacher/:id" {:keys [read-model]
                            {:keys [id]} :params
                            {:keys [post-params errors warning] :as flash} :flash}
       (let [teacher (read-model/get-teacher read-model (uuid id))
             departments (read-model/list-departments read-model)
             all-class-names (read-model/class-names-for-department read-model (:department teacher))]
         (render-edit teacher departments all-class-names post-params flash))))

(def queries
  (fn [req]
    (binding [*current-nav-uri* "/list-teachers"]
      (app req))))
