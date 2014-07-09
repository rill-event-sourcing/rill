(ns studyflow.login.system
  (:require [clojure.core.async :refer [>!!]]
            [crypto.password.bcrypt :as bcrypt]
            [rill.event-channel :refer [event-channel]]
            [rill.event-store.atom-store :as atom-store]
            [rill.event-stream :refer [all-events-stream-id]]
            [studyflow.events.student :as student-events]
            [studyflow.login.credentials :as credentials]))

(defn bootstrap! []
  (let [event-store (atom-store/atom-event-store "http://127.0.0.1:2113"
                                                 {:user "admin" :password "changeit"})
        channel (event-channel event-store all-events-stream-id -1 0)]
    (credentials/listen! channel)
    (>!! channel (student-events/credentials-added
                  "my-student-id"
                  "fred@example.com"
                  (bcrypt/encrypt "wilma")))))
