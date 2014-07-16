(ns studyflow.school-administration.main
  (:require [clojure.core.async :refer [<!! thread]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET POST defroutes routes]]
            [compojure.route :refer [not-found]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css]]
            [org.bovinegenius.exploding-fish :as uri]
            [rill.event-store.memory :refer [memory-store]]
            [rill.handler :refer [try-command]]
            [rill.message :as message]
            [rill.uuid :refer [new-id uuid]]
            [rill.web :refer [wrap-command-handler]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [ring.middleware.defaults :refer [site-defaults
                                              wrap-defaults]]
            [ring.util.request :refer [request-url]]
            [ring.util.response :as response]
            [studyflow.school-administration.read-model :as m]
            [studyflow.school-administration.read-model.event-handler :refer [handle-event]]
            [studyflow.school-administration.student :as student]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View


(def app-title "Studyflow")

(defn layout [{:keys [title flash]} & body]
  (html5
   [:head
    [:title (str/join " - " [title app-title])]
    (include-css "screen.css")]
   [:body
    [:h1 title]
    (when flash [:div.flash flash])
    [:div.container body]]))

(defn render-new-student-form
  [{:keys [full-name]}]
  (form/form-to {:class "mod-create-student"} [:post "/create-student"]
   (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
   (form/text-field {:placeholder "Full name"} "full-name" full-name)
   [:button {:type "submit"} "Add student"]))

(defn student-row
  [{:keys [id version full-name]}]
  [:tr
   [:td id]
   [:td
    (form/form-to {:class "mod-change-student-name"} [:post "/change-student-name"]
     (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
     (form/hidden-field "student-id" id)
     (form/hidden-field "expected-version" version)
     (form/text-field {:placeholder "Full name"} "full-name" full-name)
     [:button {:type "submit"} "Save"])]])

(defn render-student-list
  [students options]
  (layout
   (merge {:title "Student list"} options)
   [:table.mod-students
    [:thead
     [:tr
      [:th "id"]
      [:th "name"]]]
    [:tbody
     (map student-row students)]]
   (render-new-student-form nil)))

(defroutes queries
  (GET "/" {:keys [read-model flash]}
       (render-student-list (m/list-students read-model) {:flash flash}))
  (not-found "Nothing here"))

(defroutes commands
  (POST "/create-student" {{:keys [full-name]} :params}
        (student/create! (new-id) full-name))
  (POST "/change-student-name" {{:keys [student-id expected-version full-name]} :params}
        (student/change-name! (uuid student-id) (Long/parseLong expected-version) full-name)))

(defn add-version-to-url
  [url aggregate-id aggregate-version]
  (-> url
      (uri/param "refresh-count" 0)
      (uri/param "aggregate-id" aggregate-id)
      (uri/param "aggregate-version" aggregate-version)))

(defn wrap-back-to-previous
  [f]
  (fn [{headers :headers :as request}]
    (let [referer (or (headers "referer") "/")]
      (when-let [{:keys [status] {:keys [aggregate-version aggregate-id]} :body} (f request)]
        (cond
         (= status 200)
         (response/redirect (add-version-to-url referer aggregate-id aggregate-version))

         (= status 412) ; HTTP 412 Precondition Failed
         (assoc (response/redirect referer) :flash "record not updated; already edited by somebody else")

         :else
         {:status status :body "error" :headers { "Content-Type" "text/plain"}})))))

(defonce my-read-model (atom {}))

(defn model-up-to-date?
  [model id version]
  (<= version (or (m/aggregate-version model id) -1)))

(defn wrap-read-model
  [f model-atom]
  (fn [{{version :aggregate-version id :aggregate-id count :refresh-count} :params :as request}]
    (let [read-model @model-atom
          count (if count (Integer/parseInt count) 0)]
      (if (and version id count (< count 5)
               (not (model-up-to-date? read-model (uuid id) (Integer/parseInt version))))
        {:status 200
         :headers {"Refresh" (str "1; url=" (uri/param (request-url request) "refresh-count" (inc count)))
                   "Content-Type" "text/html"}
         :body (html5 [:body "Just a sec..."])}
        (f (assoc request :read-model read-model))))))

(def queries-app
  (-> queries
      (wrap-read-model my-read-model)))

;(defonce event-store (atom-event-store "http://127.0.0.1:2113"))
(defonce event-store (memory-store))

(defn edu-route-registration-trigger
  ;; TODO this should check if the events were already seen before.
  ;; TODO and we should probably not run more than instance of this trigger.
  [event-store event]
  (when (= (message/type event) :studyflow.login.edu-route-student.events/Registered)
    (log/info event)
    (try-command event-store (student/create-from-edu-route-credentials! (:student-id event) (:edu-route-id event) (:full-name event)))))


(defn event-listener [channel read-model-atom]
  (log/info "Starting event listener")
  (thread
    (loop []
      (log/info "loop de loop")
      (when-let [event (<!! channel)]
        (log/info (pr-str event))
        (swap! read-model-atom handle-event event)
        (edu-route-registration-trigger event)
        (recur)))))

(def commands-app
  (-> commands
      (wrap-command-handler event-store)
      wrap-back-to-previous))

(def app
  (-> (routes commands-app queries-app)
      (wrap-defaults site-defaults)))
