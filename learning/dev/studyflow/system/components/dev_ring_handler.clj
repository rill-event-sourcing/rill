(ns studyflow.system.components.dev-ring-handler
  (:require [com.stuartsierra.component :refer [Lifecycle]]
            [studyflow.learning.web :as web]
            [clojure.string :as string]
            [clojure.tools.logging :refer [info debug spy] :as log]
            [rill.event-store :as event-store]))

(defn wrap-dev-cljs [handler match replace]
  (fn [req]
    (let [res (handler req)]
      (if (and (.startsWith (get-in res [:headers "Content-Type"] "") "text/html" )
               (not (.contains ^String (str "" (get req :query-string)) "prod")))
        (-> res
            (update-in [:body]
                       (fn [body]
                         (-> body
                             (cond->
                              (not (string? body))
                              slurp)
                             (string/replace match replace))))
            (update-in [:headers] dissoc "Content-Length" "Last-Modified"))
        res))))

(defn wrap-time-track [handler event-store]
  (fn [req]
    (log/warn "wrap-time-track" req (.startsWith (:uri req) "/time"))
    (if-not (.startsWith (:uri req) "/time")
      (handler req)
      (let [section-id "98b3997f-63db-4f1d-b89e-5378e5f19514"
            student-id "2b45e104-821e-4f73-aa15-a5109267214c"
            stream-id (str "section-test:" section-id ":" student-id)
            tracking-stream-id (str "tracking:" student-id)]
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body (str "hello world!!"
                    stream-id
                    (string/join "<br/>" (event-store/retrieve-events event-store stream-id))
                    "<br>------------<br>"
                    tracking-stream-id "<br/>"
                    (string/join "<br/>" (event-store/retrieve-events event-store tracking-stream-id))
                    #_(pr-str (deref (.state (:event-store event-store)))))}))))

(defrecord DevRingHandlerComponent [event-store read-model redirect-urls cookie-domain session-store]
  Lifecycle
  (start [component]
    (info ["Starting learning dev handler with session store" session-store])
    (assoc component :handler
           (-> (web/make-request-handler (:store event-store) (:read-model read-model) redirect-urls cookie-domain session-store)
               (wrap-dev-cljs
                "<script type=\"text/javascript\" src=\"/js/studyflow.js\"></script>"
                "<script src=\"/js/react_0.9.0_local_copy.js\" type=\"text/javascript\"></script>
                 <script src=\"/js/studyflow-dev.js\" type=\"text/javascript\"></script>
                 <script type=\"text/javascript\">goog.require('studyflow.web.core');</script>
                 <script type=\"text/javascript\">studyflow.web.core._STAR_println_to_console_STAR_ = true;</script>")
               (wrap-time-track (:store event-store)))))
  (stop [component]
    (info "Stopping handler")
    component))

(defn dev-ring-handler-component [redirect-urls cookie-domain]
  (map->DevRingHandlerComponent {:redirect-urls redirect-urls :cookie-domain cookie-domain}))
