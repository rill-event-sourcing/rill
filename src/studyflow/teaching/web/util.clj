(ns studyflow.teaching.web.util
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hiccup.core :refer [h]]
            [ring.util.codec :refer [url-encode]]
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

(defn chapter-list-url [class chapter-id section-id]
  (if class
    (if chapter-id
      (if section-id
        (str "/reports/" (url-encode (:id class)) "/chapter-list/" chapter-id "/" section-id)
        (str "/reports/" (url-encode (:id class)) "/chapter-list/" chapter-id))
      (str "/reports/" (url-encode (:id class)) "/chapter-list"))
    "/reports/chapter-list"))

(defn completion-url [class meijerink]
  (if class
    (if meijerink
      (str "/reports/" (url-encode (:id class)) "/" (url-encode meijerink) "/completion")
      (str "/reports/" (url-encode (:id class)) "/completion"))
    "/reports/completion"))

(defn build-url [& {:keys [report-name class meijerink chapter-id section-id]}]
  (condp = report-name
    "chapter-list" (chapter-list-url class chapter-id section-id)
    "completion" (completion-url class meijerink)))

(defn drop-list-classes [classes current-meijerink report-name selected-class-name]
  [:div {:class "m-select-box class-select" :id "dropdown-classes"}
   [:span (if selected-class-name
            selected-class-name
            "Klas")]
   [:div.dropdown
    [:ul
     (map (fn [class]
            [:li.dropdown-list-item
             [:a.dropdown-link {:href (build-url :report-name report-name :class class :meijerink current-meijerink)}
              (:class-name class)]])
          (sort-by :class-name classes))]]])

(defn drop-list-meijerink [class meijerink-criteria report-name selected-meijerink]
  [:div {:class "m-select-box" :id "dropdown-meijerink"}
   [:span (if selected-meijerink
            selected-meijerink
            "Meijerink criteria")]
   [:div.dropdown
    [:ul
     (map (fn [meijerink]
            [:li.dropdown-list-item
             [:a.dropdown-link {:href (build-url :report-name report-name :class class :meijerink meijerink)}
              meijerink]])
          meijerink-criteria)]]])

(defn layout [{:keys [title redirect-urls]} dropdown class & body]
  (html5
   [:head
    [:title (h (str/join " - " [title app-title]))]
    [:link {:href "/favicon.ico" :rel "shortcut icon" :type "image/vnd.microsoft.icon"}]
    (include-css "/css/teaching.css")
    (include-css "//cloud.typography.com/6865512/722124/css/fonts.css")]
   [:body
    [:header#m-top-header-teaching
     [:a#logo {:href "/reports/completion"}]
     [:h1#header-title "Leraren"]
     dropdown
     [:a#learning {:href (:student redirect-urls)} "Leerling omgeving"]
     (form/form-to
      {:role "form"} [:post (:login redirect-urls)]
      [:input {:type "hidden" :name "_method" :value "DELETE"}]
      [:button {:type "submit" :id "logout-form"}])]
    [:nav#m-main-sidenav
     [:a {:href "/handleidingen"} "Handleidingen"]
     [:ul#main-container-nav
      (map (fn [[report-name label]]
             [:li.main-container-nav-list-item
              [:a.main-container-nav-tab
               {:class (str report-name (when (= report-name *current-report-name*)
                                          " selected"))
                :href (build-url :report-name report-name :class class)}
               label]])
           [["completion" "Overzicht"]
            ["chapter-list" "Hoofdstukken"]])]]
    [:section#main_teaching body]
    [:footer]
    (include-js "//code.jquery.com/jquery-2.1.1.min.js")
    (include-js "/js/dropdown.js")
    (include-js "/js/usersnap.js")]))
