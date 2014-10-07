(defproject studyflow/reporting "0.1.0-SNAPSHOT"
  :description "Reporting"
  :url "http://studyflow.nl"

  :dependencies [[compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [clojurewerkz/elastisch "2.1.0-beta4"]
                 [studyflow/studyflow "0.1.0-SNAPSHOT"]
                 [org.bovinegenius/exploding-fish "0.3.4"]
                 [clj-time "0.8.0"]]

  :main studyflow.reporting.main)
