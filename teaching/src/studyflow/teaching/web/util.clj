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

(def ^:dynamic *current-report-name* nil)

(defn drop-list-classes [classes current-meijerink report-name selected-class-name]
  [:div {:class "m-select-box class-select" :id "dropdown-classes"}
   [:span (if selected-class-name
            selected-class-name
            "Klas")]
   [:div.dropdown
    [:ul
     (map (fn [class]
            [:li.dropdown-list-item
             [:a.dropdown-link {:href
                                (if current-meijerink
                                  (str "/reports/" (:id class) "/" current-meijerink "/" report-name)
                                  (str "/reports/" (:id class) "/" report-name))}
              (:class-name class)]])
          classes)]]])

(defn drop-list-meijerink [class meijerink-criteria report-name selected-meijerink]
  [:div {:class "m-select-box" :id "dropdown-meijerink"}
   [:span (if selected-meijerink
            selected-meijerink
            "Meijerink criteria")]
   [:div.dropdown
    [:ul
     (map (fn [meijerink]
            [:li.dropdown-list-item
             [:a.dropdown-link {:href (str "/reports/" (:id class) "/" meijerink "/" report-name)}
              meijerink]])
          meijerink-criteria)]]])

(defn layout [{:keys [title redirect-urls]} dropdown selected-class-id & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (include-css "/css/teaching.css")
    (include-css "//cloud.typography.com/6865512/722124/css/fonts.css")
    (include-js "//code.jquery.com/jquery-2.1.1.min.js")
    (include-js "/js/dropdown.js")]
   [:body
    [:header#m-top-header-teaching
     [:a#logo {:href "/reports/completion"}]
     [:h1#header-title "Leraren"]
     dropdown
     [:a#learning {:href (:student redirect-urls)} "Naar Studyflow Rekenen"]
     (form/form-to
      {:role "form"} [:post (:login redirect-urls)]
      [:input {:type "hidden" :name "_method" :value "DELETE"}]
      [:button {:type "submit" :id "logout-form"}])]
    [:nav#m-main-sidenav
     [:ul#main-container-nav
      (map (fn [[report-name label]]
             [:li.main-container-nav-list-item
              [:a.main-container-nav-tab
               {:class (str report-name (when (= report-name *current-report-name*)
                                          " selected"))
                :href (if selected-class-id
                        (str "/reports/" selected-class-id "/" report-name)
                        (str "/reports/" report-name))}
               label]])
           [["completion" "Overzicht"]
            ["chapter-list" "Hoofdstukken"]])]]
    [:section#main_teaching body]
    [:footer]]))
