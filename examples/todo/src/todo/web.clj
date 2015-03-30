(ns todo.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [hiccup.core :refer [html]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.response :refer [redirect-after-post]]
            [todo.task :as task])
  (:import [java.util UUID]))

(defn layout [title body]
  (html
   [:html
    [:head
     [:title title]]
    [:body
     [:header [:h1 title]]
     body]]))

(defn render-index [tasks]
  (layout
   "index"
   [:main
    [:ul
     (for [[id {:keys [description]}] tasks]
       [:li
        [:form {:action "update-description" :method "post"}
         [:input {:type "hidden" :name "task-id" :value id}]
         [:input {:type "text" :name "description" :value description}]
         [:button "Update"]]
        [:form {:action "delete" :method "post"}
         [:input {:type "hidden" :name "task-id" :value id}]
         [:button {:onclick "return confirm('Sure?')"} "Delete"]]])]
    [:form {:action "add" :method "post"}
     [:fieldset
      [:input {:type "text" :name "description"}]
      [:button "Add"]]]]))

(defn render-error [error]
  (layout
   "error"
   [:div (pr-str error)]))

(defroutes routes
  (GET "/" []
       (render-index (task/by-id)))

  (POST "/add" [description]
        (let [[status payload] (task/create! description)]
          (if (= status :ok)
            (redirect-after-post "/")
            (render-error payload))))

  (POST "/delete" [task-id]
        (let [uuid (UUID/fromString task-id)
              [status payload] (task/delete! uuid)]
          (if (= status :ok)
            (redirect-after-post "/")
            (render-error payload))))

  (POST "/update-description" [task-id description]
        (let [uuid (UUID/fromString task-id)
              [status payload] (task/update-description! uuid description)]
          (if (= status :ok)
            (redirect-after-post "/")
            (render-error payload)))))

(def handler
  (-> routes
      wrap-params))
