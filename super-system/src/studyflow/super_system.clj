(ns studyflow.super-system
  (:require [com.stuartsierra.component :refer [system-map] :as component]
            [ring.middleware.session.memory :refer [memory-store]]
            [studyflow.web.durable-session-store :refer [durable-store]]
            [studyflow.components.memory-event-store :as memory-event-store]
            [studyflow.components.psql-event-store :refer [psql-event-store-component]]
            [studyflow.super-system.components.dev-event-fixtures :as dev-event-fixtures]
            [clojure.tools.logging :as log]
            [studyflow.school-administration.system :as school-administration]
            [studyflow.teaching.system :as teaching]
            [learning-dev-system]
            [login-dev-system]))

(def redirect-urls
  (let [root "localhost"]
    {:editor (str "http://" root ":2000")
     :student (str "http://" root ":3000")
     :login (str "http://" root ":4000")
     :teacher (str "http://" root ":4001")}))

(defn namespace-system [system prefix exceptions]
  (let [exceptions (set exceptions)
        prefix-keyword (fn [kw]
                         (if (contains? exceptions kw)
                           kw
                           (keyword (name prefix) (name kw))))]
    (zipmap (map prefix-keyword (keys system))
            (for [c (vals system)]
              (component/using
               c
               (let [deps (component/dependencies c)]
                 (zipmap (keys deps)
                         (map prefix-keyword (vals deps)))))))))


(defn make-system [config]
  (let [learning (-> (learning-dev-system/dev-system (-> (if (:psql config)
                                                           (assoc learning-dev-system/dev-config :no-fixtures true)
                                                           learning-dev-system/dev-config)
                                                         (assoc :redirect-urls
                                                           (select-keys redirect-urls [:login :learning]))))
                     (dissoc :event-store :session-store)
                     (namespace-system :learning [:event-store :session-store]))

        login (-> (login-dev-system/make-system {:jetty-port 4000
                                                 :default-redirect-paths {"editor" (:editor redirect-urls)
                                                                          "student" (:student redirect-urls)
                                                                          "teacher" (:teacher redirect-urls)}})
                  (dissoc :event-store :session-store)
                  (namespace-system :login [:event-store :session-store]))

        school-administration (-> (school-administration/prod-system {:port 5000})
                                  (dissoc :event-store)
                                  (namespace-system :school-administration [:event-store]))

        teaching (-> (teaching/prod-system {:port 4001
                                            :redirect-urls {:login (:login redirect-urls)
                                                            :teaching (:login redirect-urls)}
                                            :cookie-domain nil})
                     (dissoc :event-store :session-store)
                     (namespace-system :teaching [:event-store :session-store]))

        shared-system {:event-store (if-let [url (:psql config)]
                                      (psql-event-store-component url)
                                      (memory-event-store/memory-event-store-component))
                       :session-store (memory-store) ; (durable-store "redis://localhost:6379")
                       :dev-event-fixtures
                       (component/using
                        (dev-event-fixtures/dev-event-fixtures-component)
                        [:event-store])}]
    (-> shared-system
        (into learning)
        (into login)
        (into school-administration)
        (into teaching)
        (->>
         (mapcat identity)
         (apply system-map)))))
