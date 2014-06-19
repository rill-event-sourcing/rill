(ns rill.event-store.atom-store.cursor
  (:require [rill.event-store.atom-store
             [page :as page] [feed :as feed] [event :as event]]
            [clojure.tools.logging :as log]))

;; note that cursors are ordered oldest event first
;; while the page feed considers oldest events to be last

(defn first-cursor
  [store-uri stream-id]
  (let [head-uri (feed/head-uri store-uri stream-id)]
    {:head-uri head-uri :head? true :index 0}))

(defn next-cursor
  [{:keys [page index] :as current-cursor}]
  {:pre [page index]}
  (if (= index (dec (page/num-events page)))
    {:page-uri (page/previous-uri page) :index 0}
    {:page page :index (inc index)}))

(defn with-page
  [cursor poll-seconds]
  {:post [(or (:page cursor) (:page-uri cursor))]}
  (if (:page cursor)
    cursor
    (when-let [page (page/load-page (:page-uri cursor) poll-seconds)]
      (when (page/event-uris page)
        {:page page :index (:index cursor)}))))

(defn load-event-from-head
  [{:keys [head-uri index] :as cursor} poll-seconds]
  (let [head-page (or (:page cursor) (page/load-page head-uri poll-seconds))]
    (log/debug [:load-event-from-head head-page])
    (if-let [last-page-uri (page/last-uri head-page)]
      (let [last-page (page/load-page last-page-uri)]
        [(event/load-event (nth (page/event-uris last-page) index)) {:page last-page :index 0}])
      (when-let [uri (nth (page/event-uris head-page) index)]
        [(event/load-event uri) {:head? true :index index :page head-page}]))))

(defn load-event
  [cursor poll-seconds]
  (log/debug [:load-event cursor])
  (if (:head? cursor)
    (load-event-from-head cursor poll-seconds)
    (if-let [cursor (with-page cursor poll-seconds)]
      (let [event-uri (nth (page/event-uris (:page cursor)) (:index cursor))]
        (assert event-uri)
        [(event/load-event event-uri) cursor])
      nil)))

(defn event-seq
  ([cursor message-constructor poll-seconds]
     (lazy-seq
      (when-let [[event current-cursor] (load-event cursor poll-seconds)]
        (cons (with-meta (message-constructor event) {:cursor current-cursor})
              (event-seq (next-cursor current-cursor) message-constructor 0))))))
