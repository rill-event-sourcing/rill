(ns studyflow.login.edu-route-service)

(defprotocol EduRouteService
  (get-student-info [service edu-route-session-id signature] "returns student information or nil"))


