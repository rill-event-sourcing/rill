(ns rill.event-store.psql.pool
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

(defn open
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setJdbcUrl (str "jdbc:" spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))] 
    {:datasource cpds}))


(defn close
  [pool]
  (.close (:datasource pool)))

