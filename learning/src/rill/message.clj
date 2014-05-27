(ns rill.message
  (:refer-clojure :exclude [type]))

(defprotocol Message
  (id [this] "The unique id of the message")
  (aggregate-id [this] "The aggregate-id of the message")
  (type [this] "The type of the message as named thing (string, keyword...)")
  (data [this] "The payload of the message"))

(defprotocol SpansAggregates
  (aggregate-ids [this] "Seq of ids for commands that need additional aggregates to complete"))

(defmulti map->Message
  (fn [s m]
    s))

(defmacro defmessage
  [name params]
  `(do (defrecord ~name ~(into '[id] params)
         Message
         ~'(id [this] (:id this))
         ~(list 'aggregate-id ['this]  (list (keyword (first params)) 'this))
         ~(list 'type '[this] (str name))
         ~'(data [this] (dissoc this :id)))

       (defmethod map->Message ~(str name)
         [~'_ map#]
         (~(symbol (str "map->" name)) map#))))

(defmacro defcommand
  [name params]
  `(defmessage ~name ~params))

(defmacro defevent
  [name params]
  `(defmessage ~name ~params))


