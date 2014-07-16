(ns rill.event-store.atom-store.page
  (:require [clj-http.client :as http]))

(defn head?
  [page]
  (boolean (:headOfStream page)))

(defn relation-fn [relation]
  (fn [page]
    (:uri (first (filter #(= relation (:relation %)) (:links page))))))

(def previous-uri (relation-fn "previous"))
(def last-uri (relation-fn "last"))

(defn event-uris
  [page]
  (seq (reverse (map :id (:entries page)))))

(defn num-events
  [page]
  (count (:entries page)))

(defn uri
  [page]
  (:uri page))

(defn load-page
  ([uri poll-seconds opts]
     (let [response (http/get uri
                              (merge opts
                                     {:as :json
                                      :throw-exceptions false}
                                     (if (and poll-seconds
                                              (< 0 poll-seconds))
                                       {:headers {"ES-LongPoll" (str poll-seconds)}})))]
       (if-not (= 200 (:status response))
         nil
         (assoc (:body response) :uri uri))))
  ([uri opts]
     (load-page uri 0 opts)))
