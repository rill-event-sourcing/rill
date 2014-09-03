(ns studyflow.web.caching)

(defn no-caching
  [response]
  (update-in response [:headers] assoc
             "Cache-Control" "no-cache, must-revalidate"
             "Pragma" "no-cache"))

(defn wrap-no-caching
  [f]
  (fn [request]
    (when-let [response (f request)]
      (no-caching response))))

(defn response-can-be-cached?
  [{:keys [status headers cookies]}]
  (and (= 200 status)
       (not cookies)
       (or (not headers)
           (and (not (headers "Location"))
                (not (headers "Set-Cookie"))))))

(defn request-can-be-cached?
  [{:keys [request-method]}]
  (boolean (#{:get :head} request-method)))

(defn wrap-no-cache-dwim
  "Explicitly set no-caching headers on any request/response pair
that can not be cached (no set-cookies, POSTs, redirectes etc)"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (not (and (request-can-be-cached? request)
                    (response-can-be-cached? response)))
        (no-caching response)
        response))))


