(ns studyflow.web.core
  (:require [goog.dom :as gdom]
            [goog.dom.classes :as gclasses]
            [goog.string :as gstring]
            [goog.events :as gevents]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [studyflow.web.calculator :as calculator]
            [studyflow.web.history :refer [navigate-to-path]]
            [goog.Timer :as gtimer]
            [studyflow.web.service :as service]
            [studyflow.web.helpers :as helpers]
            [studyflow.web.recommended-action :refer [recommended-action]]
            [clojure.walk :as walk])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def ^:dynamic *println-to-console* false)

(set! *print-newline* false)
(set! *print-fn*
      (fn [& args]
        (when *println-to-console*
          (.apply (.-log js/console) js/console (into-array args)))))

(defn course-id-for-page []
  (.-value (gdom/getElement "course-id")))

(defn student-id-for-page []
  (.-value (gdom/getElement "student-id")))

(defn student-full-name-for-page []
  (.-value (gdom/getElement "student-full-name")))

(defn logout-target-for-page []
  (.-value (gdom/getElement "logout-target")))

(defn teaching-url-for-page []
  (let [url (.-value (gdom/getElement "teaching-target"))]
    (when (seq url)
      url)))

(defn init-app-state []
  (atom {:static {:course-id (course-id-for-page)
                  :student {:id (student-id-for-page)
                            :full-name (student-full-name-for-page)}
                  :logout-target (logout-target-for-page)
                  :teaching-url (teaching-url-for-page)}
         :view {:selected-path {:chapter-id nil
                                :section-id nil
                                :main :dashboard
                                :section-tab nil}
                :calculator-position {:left 100 :top 100}
                :calculator-light-mode? false}
         :aggregates {}}))

(defn watch-notifications!
  [notification-channel cursor]
  (go (loop []
        (when-let [event (<! notification-channel)]
          (case (:type event)
            "studyflow.learning.section-test.events/QuestionAssigned"
            (calculator/reset-calculator)
            "studyflow.learning.section-test.events/QuestionAnsweredIncorrectly"
            (do (om/update! cursor
                            [:view :shake-class]
                            "shake")
                (gtimer/callOnce
                 (fn []
                   (om/update! cursor
                               [:view :shake-class]
                               nil))
                 300))

            "studyflow.learning.chapter-quiz.events/QuestionAssigned"
            (om/update! cursor [:view :chapter-quiz (:chapter-id event) :test :questions] {})

            "studyflow.learning.chapter-quiz.events/Stopped"
            (navigate-to-path {:main :dashboard
                               :chapter-id (:chapter-id event)
                               :section-id nil})
            nil)
          (recur)))))
