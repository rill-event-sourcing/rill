(ns studyflow.login.main
  (:require [clojure.string :as str]
            [compojure.core :refer [defroutes GET]]
            [hiccup.page :refer [html5]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View

(def app-title "Studyflow login")

(defn layout [title & body]
  (html5
   [:head
    [:title (str/join "-" [app-title title])]]
   [:body
    [:h1 title]
    body]))

(defn home [user-count]
  [:div
   [:h2 "welcome home"]
   [:div
    (str user-count " users registered")]])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Model

(defn count-users [db]
  5)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controller

(defroutes actions
  (GET "/" {db :db}
       (layout "HOME" (home (count-users db)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiring

(def app
  (wrap-defaults actions site-defaults))
