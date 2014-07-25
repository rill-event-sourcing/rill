(defproject studyflow/login "0.1.0-SNAPSHOT"
  :description "Authentication hub"
  :url "http://studyflow.nl/"

  :dependencies [[compojure "1.1.8"]
                 [com.stuartsierra/component "0.2.1"]
                 [com.taoensso/carmine "2.6.2"]
                 [crypto-password "0.1.3"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [nl.studyflow/eduroute-api "0.0.1"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [studyflow/components "0.1.0-SNAPSHOT"]
                 [digest "1.4.4"]]

  :profiles {:dev {:source-paths ["dev"]
                   :resource-paths ["dev/resources"]
                   :dependencies [[org.clojure/tools.trace "0.7.5"]
                                  [org.clojure/tools.namespace "0.2.5"]
                                  [enlive "1.1.5"]
                                  [ring-mock "0.1.5"]]}
             :uberjar {:aot [studyflow.login.launcher]
                       :main studyflow.login.launcher}})
