(ns rill.aggregate)

(defmulti handle-command
  "Take a command and return a seq of events or nil on error"
  (fn [command & _]
    (class command)))

(defmulti handle-event
  "Take an event and return the new state of the aggregate"
  (fn [aggregate event]
    [(class aggregate) (class event)]))

(defn update-aggregate
  [aggregate events]
  (reduce handle-event aggregate events))

(defn load-aggregate
  [events]
  (update-aggregate nil events))

(defmacro defaggregate [name attrs]
  `(defrecord ~name ~(into '[id] attrs)))

(defmacro on-command [[aggregate-class command-class] & body]
  `(defmethod handle-command [~aggregate-class ~command-class] [~'this ~'command]
     ~@body))
