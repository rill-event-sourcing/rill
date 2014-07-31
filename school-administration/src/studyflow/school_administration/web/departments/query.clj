(ns studyflow.school-administration.web.departments.query
  (:require [compojure.core :refer [GET defroutes]]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [rill.uuid :refer [uuid]]
            [studyflow.school-administration.read-model :as read-model]
            [studyflow.school-administration.web.html-util :refer [*current-nav-uri*
                                                                   anti-forgery-field
                                                                   field-errors
                                                                   layout]]))

(defn- cancel-button [school-id]
  [:a.button.cancel {:href (str "/edit-school/" school-id)} "Cancel"])

(defn render-new
  [{school-id :id school-name :name :as school} {:keys [name]} {:keys [errors] :as options}]

  (layout
   (merge {:title (str (h school-name) " - New department")} options)

   (form/form-to
    [:post (str "/create-department/" school-id)]

    [:fieldset
     (anti-forgery-field)
     [:div.field
      (form/label "name" "Name")
      (form/text-field {:placeholder "ex. Kerkstraat"} "name" name)
      (field-errors (:name errors))]
     [:div.actions
      [:button.primary {:type "submit"} "Add department"]
      (cancel-button school-id)]])))

(defn render-edit
  [{school-id :id school-name :name :as school} department post-params {:keys [errors] :as options}]

  (let [{:keys [id version name sales-data]} (merge department post-params)
        {original-name :name} department]
    (layout
     (merge {:title (str school-name " - Edit department: " original-name)} options)

     (form/form-to
      [:post (str "/change-department-name/" school-id)]

      [:fieldset
       (anti-forgery-field)
       (form/hidden-field "department-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "name" "Name")
        (form/text-field {:placeholder "ex. Kerkstraat"} "name" name)
        (field-errors (:name errors))]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button school-id)]])

     (form/form-to
      [:post (str "/change-department-sales-data/" school-id)]

      [:fieldset
       [:legend "Sales data"]
       (anti-forgery-field)
       (form/hidden-field "department-id" id)
       (form/hidden-field "expected-version" version)
       [:div.field
        (form/label "licenses-sold" "Licenses sold")
        (form/text-field {:type "number"} "licenses-sold" (:licenses-sold sales-data))
        (field-errors (:licenses-sold errors))]
       [:div.field
        (form/label "status" "Status")
        (form/drop-down "status" ["None", "Pilot", "Client"] (:status sales-data))
        (field-errors (:status errors))]
       [:div.actions
        [:button.primary {:type "submit"} "Update"]
        (cancel-button school-id)]])

     (form/form-to
      [:post (str "/import-students/" school-id)]
      [:fieldset
       [:legend "Import students"]
       (anti-forgery-field)
       (form/hidden-field "department-id" id)
       (form/hidden-field "expected-version version")
       [:div.field
        (form/label "student-data" "Student data; copy and paste from Excel") [:br]
        (form/text-area {:class "tab-delimited-data" :wrap "off"} "student-data")]
       [:div.actions
        [:button.primary {:type "submit"} "Import new students"]
        (cancel-button school-id)]]))))


(defroutes app
  (GET "/new-department/:school-id" {:keys [read-model flash]
                                     {:keys [school-id]} :params}
       (let [school-id (uuid school-id)
             school (read-model/get-school read-model school-id)]
         (render-new school (:post-params flash) flash)))
  (GET "/edit-department/:school-id/:id" {:keys [read-model]
                                          {:keys [school-id id]} :params
                                          {:keys [post-params errors warning] :as flash} :flash}
       (let [school-id (uuid school-id)
             school (read-model/get-school read-model school-id)
             id (uuid id)
             department (read-model/get-department read-model id)]
         (render-edit school department post-params flash))))

(def queries
  (fn [req]
    (binding [*current-nav-uri* "/list-schools"]
      (app req))))
