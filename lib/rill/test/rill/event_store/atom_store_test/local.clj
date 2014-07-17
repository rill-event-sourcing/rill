(ns rill.event-store.atom-store-test.local
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [rill.event-store.atom-store :refer [atom-event-store]]
            [me.raynes.conch.low-level :as sh]
            [me.raynes.conch :refer [programs]])
  (:import (java.io File)))

(programs rm)

(defn local-atom-store
  []
  (if-let [cmd (env :atom-event-store-command)]
    (str/split cmd #" +")))

(defn local-atom-store-start-command
  [log-file port-number]
  (if-let  [cmd (local-atom-store)]
    (map str (concat cmd ["--ip=127.0.0.1" "-h" port-number "-d" log-file]))))

(defn start-local-atom-store
  []
  (println "STARTING LOCAL STORE")
  (if (local-atom-store)
    (let [tmpfile (File/createTempFile "atom-store-test" ".db")
          port 39287]
      (.delete tmpfile)
      (.deleteOnExit tmpfile)
      (if-let [cmd (local-atom-store-start-command (.getAbsolutePath tmpfile) port)]
        (let [proc (apply sh/proc cmd)]
          (future (sh/stream-to-out proc :out))
          {:cmd cmd
           :proc proc
           :tmpfile tmpfile
           :url (format "http://127.0.0.1:%d" port)})))
    (do (println "ATOM_EVENT_STORE_COMMAND environment variable is not set.
Not running tests against the atom event store.")
        nil)))

(defn stop-local-atom-store
  [local-store]
  (sh/destroy (:proc local-store))
  (let [tmpdir (.getAbsolutePath (:tmpfile local-store))]
    (when (and (.startsWith tmpdir "/")
               (< 4 (count tmpdir)))
      (rm "-rf" tmpdir))))

(defmacro with-local-atom-store
  [[store-binding] & body]
  `(if-let [store# (start-local-atom-store)]
     (try
       (Thread/sleep 20000)
       (let [~store-binding (atom-event-store (:url store#) {:user "admin" :password "changeit"})]
         ~@body)
       (finally
         (stop-local-atom-store store#)))))
