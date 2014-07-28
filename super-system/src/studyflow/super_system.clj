(ns studyflow.super-system
  (:require [com.stuartsierra.component :refer [system-map] :as component]
            [studyflow.login.system :as login]
            [studyflow.system :as learning]
            [studyflow.system.components.memory-event-store :as memory-event-store]
            [clojure.tools.logging :as log]
            [learning-dev-system]))

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

(defn make-system [_]
  (let [learning (-> (learning-dev-system/dev-system (assoc learning-dev-system/dev-config
                                                       :jetty-port 3000))
                     (dissoc :event-store)
                     (namespace-system :learning [:event-store]))
        login (-> (login/make-system {:jetty-port 4000
                                      :default-redirect-paths {"editor" "http://localhost:2000"
                                                               "student" "http://localhost:3000"}
                                      :session-max-age (* 8 60 60)
                                      :cookie-domain nil})
                  (dissoc :event-store)
                  (namespace-system :login [:event-store]))
        shared-system {:event-store (component/using
                                     (memory-event-store/memory-event-store-component)
                                     [])}]
    (-> shared-system
        (into learning)
        (into login)
        (->>
         (mapcat identity)
         (apply system-map)))))
