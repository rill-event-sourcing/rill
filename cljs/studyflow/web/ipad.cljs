(ns studyflow.web.ipad
  (:require [om.core :as om]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [goog.events :as gevents]))


(def ipad? (js/navigator.userAgent.match #"iPhone|iPad|iPod"))

(defn ipad-scroll-on-inputs-blur-fix []
  ;; fixed header and side-nav move around in ipad upon focusing an
  ;; input box (and showing the keyboard), this unfocuses the input
  ;; when touching outside it, and that makes the ipad reset its layout
  (when ipad?
    (let [app (gdom/getElement "app")]
      (gevents/listen app
                      "touchstart"
                      (fn [e]
                        ;; without this you can't onclick the button
                        ;; in the tooltip anymore
                        (when (let [node-name (.. e -target -nodeName)]
                                (and (not= node-name "INPUT")
                                     (not= node-name "BUTTON")))
                          (.blur js/document.activeElement))
                        true)))))

(defn ipad-reset-header []
  (when ipad?
    (js/window.scrollTo js/document.body.-scrollLeft js/document.body.-scrollTop)))

(defn ipad-fix-scroll-after-switching []
  ;; when switching between explantion and questions, if you are
  ;; scrolled down a lot in explanation the question will be scrolled
  ;; of screen whn switching to questions
  (when ipad?
    (js/document.body.scrollIntoViewIfNeeded)))
