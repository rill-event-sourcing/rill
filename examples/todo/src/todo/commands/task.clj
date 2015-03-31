(ns todo.commands.task
  (:require [clojure.string :refer [blank?]]
            [rill.aggregate :refer [handle-command]]
            [rill.message :refer [defcommand]]
            [schema.core :as schema]
            [todo.aggregates.task :as task]))

(defcommand Create!
  :task-id schema/Uuid
  :description schema/Str)

(defmethod handle-command ::Create!
  [_ {:keys [task-id description]}]

  (cond
    (blank? description)
    [:rejected [:description ["can't be blank"]]]

    :else
    [:ok [(task/created task-id description)]]))

(defcommand Delete!
  :task-id schema/Uuid)

(defmethod handle-command ::Delete!
  [_ {:keys [task-id]}]
  [:ok [(task/deleted task-id)]])

(defcommand UpdateDescription!
  :task-id schema/Uuid
  :description schema/Str)

(defmethod handle-command ::UpdateDescription!
  [task {:keys [task-id description]}]

  (cond
    (blank? description)
    [:rejected [:description ["can't be blank"]]]

    (= (:description task) description)
    [:rejected [:description ["nothing changed"]]]

    :else
    [:ok [(task/description-updated task-id description)]]))
