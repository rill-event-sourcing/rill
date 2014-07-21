(ns studyflow.login.edu-route-service)

(defprotocol EduRouteService
  (check-edu-route-signature [service edu-route-session-id signature] "checks the signature against a md5")
  (get-student-info [service edu-route-session-id] "returns student information or nil")
  (get-school-info [service assu-nr brin-code] "returns school information or nil"))


