(ns studyflow.system.components.fixtures-loading
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [ring.mock.request :as ring-mock]
            [clout-link.route :as route]
            [studyflow.web.routes :as routes]
            [clojure.tools.logging :refer [info debug spy]]))

(defrecord FixturesLoadingComponent [ring-handler]
  Lifecycle
  (start [component]
    (info "Starting fixtures-loading-component")
    (let [handler (:handler ring-handler)
          materials (slurp "test/studyflow/material.json")
          course-id (let [[_ rest] (.split ^String materials ":")
                          [quoted-id] (.split ^String rest ",")
                          [_ id] (.split ^String quoted-id "\"")]
                      id)]
      (handler (-> (ring-mock/request :put (route/uri-for routes/update-course-material course-id)
                                      materials)
                   (ring-mock/content-type "application/json"))))
    component)
  (stop [component]
    (info "Stopping fixtures-loading-component, not removing anything")
    component))

(defn fixtures-loading-component []
  (map->FixturesLoadingComponent {}))
