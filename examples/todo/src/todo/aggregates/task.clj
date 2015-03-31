(ns todo.aggregates.task
  (:require [rill.aggregate :refer [handle-event]]
            [rill.message :refer [defevent]]
            [schema.core :as schema]))

(defrecord Task [task-id description])

(defevent Created
  :task-id schema/Uuid
  :description schema/Str)

(defmethod handle-event ::Created
  [_ {:keys [task-id description]}]
  (->Task task-id description))

(defevent Deleted
  :task-id schema/Uuid)

(defmethod handle-event ::Deleted
  [_ {:keys [task-id]}]
  nil)

(defevent DescriptionUpdated
  :task-id schema/Uuid
  :description schema/Str)

(defmethod handle-event ::DescriptionUpdated
  [task {:keys [task-id description]}]
  (assoc task :description description))
