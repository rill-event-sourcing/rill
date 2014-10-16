(ns studyflow.migrations.active-migrations
  (:require [studyflow.migrations.wrap-migrations :refer [wrap-migrations]]
            [studyflow.migrations.2014-08-14-add-timestamps-to-events :refer [add-timestamp start-timestamp-at]]
            [studyflow.migrations.2014-08-26-credentials-claims-moved :refer [move-credentials-claims]]))

(def active-migrations
  (comp move-credentials-claims
        #(add-timestamp % start-timestamp-at)))

(defn wrap-active-migrations
  [wrapped-event-store]
  (wrap-migrations wrapped-event-store active-migrations))


