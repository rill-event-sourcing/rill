(ns rill.message
  (:refer-clojure :exclude [type])
  (:require [schema.macros :as sm]
            [schema.core :as s]))

(defn lisp-name
  "convert a camelCaseName to a dashed-name"
  [^String name]
  (apply str (Character/toLowerCase (first name))
         (mapcat #(if (Character/isUpperCase %)
                    ["-" (Character/toLowerCase %)]
                    [%]) (rest name))))

(def id ::id)
(def type ::type)

(defn data
  [m]
  (dissoc m ::id ::type))

(defmulti strict-map->Message
  (fn [s m]
    s))

(defn ->type-keyword
  [sym]
  (keyword (lisp-name (name sym))))

(defn params->args
  [params]
  (mapv #(symbol (name (first %)))
        (partition-all 2 params)))


(defmacro defmessage
  [name & params]
  {:pre [(every? keyword? (take-nth 2 params))]}
  `(do (def ~name ~(into {::id s/Uuid
                          ::type (->type-keyword name)}
                         (map vec (partition-all 2 params))))

       (defn ~(symbol (str "map->" (clojure.core/name name)))
         [params#]
         (assoc params# ::type ~(->type-keyword name)))

       ~(let [args (params->args params)
              ks (mapv keyword args)
              id-arg (gensym "msg_id_")]
          `(defn ~(symbol (str "->" (clojure.core/name name)))
             ~(vec (into [id-arg] args))
             ~(into {::id id-arg
                     ::type (->type-keyword name)}
                    (zipmap ks args))))))

(defmacro defcommand
  [name & params]
  `(defmessage ~name ~@params))

(defmulti stream-id "The id of the event stream for this message" ::type)

(defmacro defevent
  [name & params]
  `(do (defmessage ~name ~@params)
       (defmethod stream-id
         ~(->type-keyword name)
         [message#]
         (~(keyword (first params)) message#))))
