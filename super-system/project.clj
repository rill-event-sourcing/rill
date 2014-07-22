(defproject studyflow/super-system "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [studyflow/login "0.1.0-SNAPSHOT"]
                 [studyflow/learning "0.1.0-SNAPSHOT"]
                 #_[studyflow/school-administration "0.1.0-SNAPSHOT"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]]
  :profiles {:dev
             {:source-paths ["dev"]
              :resource-paths ["dev/resources"]
              :dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [org.clojure/tools.namespace "0.2.5"]
               ;; :dev :dependencies from checkouts not available in
               ;; this project but will be reloaded by c.t.n.repl/refresh
               ;; need to find a way to do this programmatically
               ;; from learning
               ;; from login
               [enlive "1.1.5"]
               [ring-mock "0.1.5"]

]}})





