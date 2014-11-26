(ns studyflow.web.coins
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn coins-header
  [cursor owner]
  (reify om/IRender
    (render [_]
      (dom/button #js {:id "coins_heading"}
                  (get-in cursor [:view :course-material :total-coins])
                  " $"))))
