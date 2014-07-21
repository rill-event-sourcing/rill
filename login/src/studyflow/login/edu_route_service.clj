(ns studyflow.login.edu-route-service)

(defprotocol EduRouteService
  (get-student-info [service edu-route-session-id signature] "returns student information or nil")
  (get-school-info [service assu-nr brin-code] "returns school information or nil"))


