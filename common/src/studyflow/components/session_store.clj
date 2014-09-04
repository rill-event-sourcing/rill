(ns studyflow.components.session-store)

(defprotocol SessionStore
  (get-user-id [store session-id] "returns whether a given session-id is valid")
  (get-role [store session-id])
  (create-session [store user-id role session-max-age])
  (delete-session! [store session-id]))
