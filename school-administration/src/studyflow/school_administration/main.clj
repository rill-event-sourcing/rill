(ns studyflow.school-administration.main
  (:require
   [clojure.core.async :refer [thread <!!]]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [compojure.core :refer [defroutes GET POST DELETE routes]]
   [compojure.route :refer [not-found]]
   [hiccup.page :refer [html5 include-css]]
   [hiccup.element :as element]
   [hiccup.form :as form]
   [org.bovinegenius.exploding-fish :as uri]
   [rill.event-store.memory :refer [memory-store]]
   [rill.uuid :refer [new-id uuid]]
   [rill.web :refer [wrap-command-handler]]
   [ring.util.response :as response]
   [ring.util.request :refer [request-url]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
   [studyflow.school-administration.read-model :as m]
   [studyflow.school-administration.read-model.event-handler :refer [handle-event]]
   [studyflow.school-administration.student :as student]
   [studyflow.school-administration.student.events :refer [fixture]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View


(def app-title "Studyflow")

(defn student-row
  [{:keys [id version full-name]}]
  [:tr
   [:td id]
   [:td full-name]
   [:td.change-name
    (form/form-to
     [:post "/change-student-name"]
     (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
     (form/hidden-field "student-id" id)
     (form/hidden-field "expected-version" version)
     (form/text-field {:placeholder "Full name"} "full-name" full-name)
     [:button {:type "submit"} "Change student name"])]])

(defn layout [title & body]
  (html5
   [:head
    [:title (str/join " - " [app-title title])]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    (include-css "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/css/bootstrap.css")
    (include-css "screen.css")
    "<!-- [if lt IE 9>]"
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/html5shiv/3.7/html5shiv.js"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/respond.js/1.3.0/respond.js"}]
    "<! [endif]-->"]
   [:body
    [:div.container body]
    "<!-- /container -->"
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/jquery/2.0.3/jquery.min.js"}]
    [:script {:src "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.1.0/js/bootstrap.min.js"}]]))

(defn render-new-student-form
  [{:keys [full-name]}]
  (form/form-to
   {:role "form"} [:post "/create-student"]
   (form/hidden-field "__anti-forgery-token" *anti-forgery-token*)
   (form/text-field {:placeholder "Full name"} "full-name" full-name)
   [:button {:type "submit"} "Add student"]))

(defn render-student-list
  [students]
  (layout
   "Student list"
   [:table
    [:thead
     [:tr
      [:th "id"]
      [:th "name"]]]
    [:tbody
     (map student-row students)]
    (render-new-student-form nil)]))

(defroutes queries
  (GET "/" {:keys [read-model]}
       (render-student-list (m/list-students read-model)))
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
         {:status status :body "out of date" :headers { "Content-Type" "text/plain"}}

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

(defonce event-store (memory-store))

(defn event-listener [channel read-model-atom]
  (thread
    (loop []
      (when-let [event (<!! channel)]
        (swap! read-model-atom handle-event event)
        (recur)))))

(def commands-app
  (-> commands
      (wrap-command-handler event-store)
      wrap-back-to-previous))

(def app
  (-> (routes commands-app queries-app)
      (wrap-defaults site-defaults)))
