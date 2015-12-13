(ns rill.message
  (:refer-clojure :exclude [type])
  (:require [schema.macros :as sm]
            [schema.core :as s]
            [rill.uuid :refer [new-id]]
            [rill.timestamp :refer [now]]
            [nl.zeekat.identifiers :refer [lisp-name]]))

(def id
  "The unique identifier of a message"
  ::id)

(def type
  "The type of the message"
  ::type)

(def number
  "The ordering number of the event in its original stream"
  ::number)

(def cursor
  "The ordering number of the event in the current stream
This may differ from message/number if the current stream is the all-event-stream"
  ::cursor)

(def timestamp
  "The creation time of the event"
  ::timestamp)

(def stream-id
  "The stream identifier for this message."
  ::stream-id)

(defn data
  [m]
  (dissoc m id type number))

(defn ->type-keyword
  [ns sym]
  (keyword (name (ns-name ns)) (name sym)))

(defn params->args
  [params]
  (mapv #(symbol (name (first %)))
        (partition-all 2 params)))

(defmulti primary-aggregate-id
  "The id of the aggregate that will handle this message"
  type)

(defmulti observers
  "A collection of [id, handler-fn] pairs for each aggregate that should receive this message after it is successfully committed."
  type)

(defmethod observers
  :default
  [_]
  nil)

(defn make-message
  "Create a new message with type `message-type` and data"
  [message-type data]
  (assoc data
    type message-type
    id (new-id)
    timestamp (now)))

(defmacro message
  [type-keyword & params]
  {:pre [(every? keyword? (take-nth 2 (butlast params)))]}
  (let [primary-aggregate-id-fn (if (even? (count params))
                                  (keyword (first params))
                                  (last params))
        params (if (even? (count params))
                 params
                 (butlast params))
        args (params->args params)
        ks (map keyword args)]
    `(do
       (defmethod primary-aggregate-id
         ~type-keyword
         [message#]
         (~primary-aggregate-id-fn message#))
       (fn ~(vec args)
         (make-message ~type-keyword ~(zipmap ks args))))))

(defmacro defmessage
  [name & params]
  (let [type-keyword (->type-keyword (ns-name *ns*) name)
        name-str (clojure.core/name name)]
    `(def
       ~(vary-meta (symbol (lisp-name name-str)) assoc
                   :doc (str "Create a new " name-str " message from the positional arguments. Automatically generates a new message id."))
       (message ~type-keyword ~@params))))

(defmacro command [& args] `(message ~@args))
(defmacro event [& args] `(message ~@args))
(defmacro defcommand
  [name & params]
  `(defmessage ~name ~@params))
(defmacro defevent
  [name & params]
  `(defmessage ~name ~@params))