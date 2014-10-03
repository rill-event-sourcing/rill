(ns studyflow.teaching.web.util
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hiccup.core :refer [h]]
            [hiccup.form :as form]
            [hiccup.page :refer [html5 include-css include-js]]))

(def app-title "Studyflow")

(defn completion-percentage [{:keys [finished total]}]
  (str (Math/round (float (/ (* finished 100) total))) "%"))

(defn completion-title [{:keys [finished total]}]
  (str finished "/" total))

(defn completion-html [{:keys [total] :as completion}]
  (if (and completion (> total 0))
    [:span {:title (completion-title completion)}
     (completion-percentage completion)]
    "&mdash;"))

(defn time-spent-str [secs]
  (if (or (nil? secs)
          (zero? secs))
    "--"
    (str (let [hours (long (Math/floor (/ secs (* 60 60))))]
           (when (pos? hours)
             (str (if (< hours 10)
                    (str "0" hours)
                    hours) ":")))
         (let [mins (rem (long (Math/floor (/ secs 60))) 60)]
           (if (< mins 10)
             (str "0" mins)
             mins))
         ":"
         (let [secs (rem secs 60)]
           (if (< secs 10)
             (str "0" secs)
             secs)))))

(defn time-spent-html [secs]
  [:span {:title (str secs " seconden")}
   (time-spent-str secs)])

(defn classerize [s]
  (-> s
      str
      str/lower-case
      (str/replace #"[^a-z ]" "")
      (str/replace #"\s+" "-")))

(def ^:dynamic *current-nav-uri* nil)

(defn drop-list-classes [classes current-meijerink report-name]
  [:div {:class "m-select-box" :id "dropdown-classes"}
   [:span "Klas"]
   [:ul.dropdown
    (map (fn [class]
           [:li.dropdown-list-item
            [:a.dropdown-link {:href
                               (if current-meijerink
                                 (str "/reports/" (:id class) "/" current-meijerink "/" report-name)
                                 (str "/reports/" (:id class) "/" report-name))}
             (:class-name class)]])
         classes)]])

(defn drop-list-meijerink [class meijerink-criteria report-name]
  [:div {:class "m-select-box" :id "dropdown-meijerink"}
   [:span "Meijerink criteria"]
   [:ul.dropdown
    (map (fn [meijerink]
           [:li.dropdown-list-item
            [:a.dropdown-link {:href (str "/reports/" (:id class) "/" meijerink "/" report-name)}
             meijerink]])
         meijerink-criteria)]])

(defn layout [{:keys [title warning message redirect-urls]} & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (include-css "/css/teaching.css")
    (include-js "//code.jquery.com/jquery-2.1.1.min.js")
    (include-js "/dropdown.js")
    ]
   [:body
    [:header#m-top_header
     [:h1#logo "Leraren"]
     (when redirect-urls
       (form/form-to
        {:role "form" :id "logout-form"} [:post (:login redirect-urls)]
        [:input {:type "hidden" :name "_method" :value "DELETE"}]
        [:button {:type "submit"} "Uitloggen"]))]
    [:section#main_teaching
     [:nav#main_container_nav
      [:ul
       (map (fn [[url label]]
              [:li.main_container_nav_list_item
               [:a.main_container_nav_tab (if (= url *current-nav-uri*) {:class "selected"} {:href url}) label]])
            [["/reports/completion" "Overzicht"]
             ["/reports/chapter-list" "Hoofdstukken"]])]]
     [:div.body
      (when warning [:div.warning (h warning)])
      (when message [:div.message (h message)])
      [:div.container body]]]
    [:footer]]))
