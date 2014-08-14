(ns studyflow.web.om-helpers
  (:require [om.dom :as dom]))

(defn raw-html
  [raw]
  (dom/span #js {:dangerouslySetInnerHTML #js {:__html raw}} nil))
