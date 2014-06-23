(ns rill.message
  (:refer-clojure :exclude [type])
  (:require [schema.macros :as sm]
            [schema.core]
            [miner.tagged :as tagged]))

(defprotocol Message
  (id [this] "The unique id of the message")
  (type [this] "The type of the message as named thing (string, keyword...)")
  (data [this] "The payload of the message"))

(defmulti strict-map->Message
  (fn [s m]
    s))

(defmacro defcommand
  [name params]
  `(do (sm/defrecord ~name ~(into '[id :- schema.core/Uuid] params)
         Message
         ~'(id [this] (:id this))
         ~(list 'type '[this] (str name))
         ~'(data [this] (dissoc this :id)))

       (defmethod print-method ~name ~'[this w] (tagged/pr-tagged-record-on ~'this ~'w))
       
       (defmethod strict-map->Message ~(str name)
         [~'_ map#]
         (~(symbol (str "strict-map->" name)) map#))))

(defprotocol Event
  (stream-id [this] "The id of the event stream for this message"))

(defmacro defevent
  [name params]
  `(do (sm/defrecord ~name ~(into '[id :- schema.core/Uuid] params)
         Message
         ~'(id [this] (:id this))
         ~(list 'type '[this] (str name))
         ~'(data [this] (dissoc this :id))
         Event
         ~(list 'stream-id '[this] (first params)))

       (defmethod print-method ~name ~'[this w] (tagged/pr-tagged-record-on ~'this ~'w))

       (defmethod strict-map->Message ~(str name)
         [~'_ map#]
         (~(symbol (str "strict-map->" name)) map#))))

