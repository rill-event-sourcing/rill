(ns studyflow.web.coins
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :as async]))

(defn coins-header
  [cursor owner]
  (reify om/IRender
    (render [_]
      (let [student-id (get-in cursor [:static :student :id])
            course-id (get-in cursor [:static :course-id])]
        (dom/div #js {:className "m-gaming"}
                 (dom/button #js {:onClick (fn []
                                             (let [open? (om/get-state owner :open?)]
                                               (om/set-state! owner :open? (not open?))
                                               (when-not open?
                                                 (-> (om/get-shared owner [:data-channel])
                                                     (async/put! ["data/leaderboard" course-id student-id])))))
                                  :className "btn yellow"}
                             (get-in cursor [:view :course-material :total-coins])
                             )
                 (when (om/get-state owner :open?)
                   (if-let [data (get-in cursor [:view :leaderboard :data])]
                     (dom/div #js {:className "leaderboard-dropdown"}
                     (dom/h2 #js {:className "leaderboard-heading"} "Meeste" (dom/span #js {:className "inline-coin"}) " de afgelopen 7 dagen")
                                (dom/table #js {:className "leaderboard-table"}
                                (apply dom/tbody nil
                                       (map (fn [[pos id coins full-name]]
                                              (dom/tr #js {:className (str "leaderboard-row" (when (= id student-id)
                                                                                               " me"))}

                                                      (dom/td #js {:className "position"} pos ".")
                                                      (dom/td nil full-name)
                                                      (dom/td #js {:className "coins"} coins)))
                                            data))))
                     "laden....")))))))
