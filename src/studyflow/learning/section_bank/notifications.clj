(ns studyflow.learning.section-bank.notifications
  (:require [rill.message :as message]))

(defmulti notify
  (fn [section-bank event & aggregates]
    (message/type event)))
