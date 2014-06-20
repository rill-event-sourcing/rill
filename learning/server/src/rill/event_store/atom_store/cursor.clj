(ns rill.event-store.atom-store.cursor
  (:require [clojure.tools.logging :as log]
            [rill.event-store.atom-store.event :as event]
            [rill.event-store.atom-store.feed :as feed]
            [rill.event-store.atom-store.page :as page]))

;; note that cursors are ordered oldest event first
;; while the page feed considers oldest events to be last

(defn first-cursor
  [store-uri stream-id client-opts]
  (let [head-uri (feed/head-uri store-uri stream-id)]
    {:head-uri head-uri :head? true :index 0 :client-opts client-opts}))

(defn next-cursor
  [{:keys [page index client-opts] :as current-cursor}]
  {:pre [page index]}
  (if (= index (dec (page/num-events page)))
    {:page-uri (page/previous-uri page) :index 0 :client-opts client-opts}
    {:page page :index (inc index) :client-opts client-opts}))

(defn with-page
  [{:keys [index page page-uri client-opts] :as cursor} poll-seconds]
  {:post [(or page page-uri)]}
  (if page
    cursor
    (when-let [page (page/load-page page-uri poll-seconds client-opts)]
      (when (page/event-uris page)
        {:page page :index index :client-opts client-opts}))))

(defn load-event-from-head
  [{:keys [head-uri index client-opts] :as cursor} poll-seconds]
  (let [head-page (or (:page cursor) (page/load-page head-uri poll-seconds client-opts))]
    (when head-page
      (log/debug [:load-event-from-head head-page])
      (if-let [last-page-uri (page/last-uri head-page )]
        (let [last-page (page/load-page last-page-uri 0 client-opts)]
          [(event/load-event (nth (page/event-uris last-page) index) client-opts) {:page last-page :index 0 :client-opts client-opts}])
        (when-let [uri (nth (page/event-uris head-page) index)]
          [(event/load-event uri client-opts) {:head? true :index index :page head-page :client-opts client-opts}])))))

(defn load-event
  [cursor poll-seconds]
  (log/debug [:load-event cursor])
  (if (:head? cursor)
    (load-event-from-head cursor poll-seconds)
    (if-let [cursor (with-page cursor poll-seconds)]
      (let [event-uri (nth (page/event-uris (:page cursor)) (:index cursor))]
        (assert event-uri)
        [(event/load-event event-uri (:client-opts cursor)) cursor])
      nil)))

(defn event-seq
  ([cursor message-constructor poll-seconds]
     (lazy-seq
      (when-let [[event current-cursor] (load-event cursor poll-seconds)]
        (log/debug [event current-cursor])
        (cons (with-meta (message-constructor event) {:cursor current-cursor})
              (event-seq (next-cursor current-cursor) message-constructor 0))))))
