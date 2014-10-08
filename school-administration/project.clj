(defproject studyflow/school-administration "0.1.0-SNAPSHOT"
  :description "School administration"
  :url "http://studyflow.nl"

  :dependencies [[compojure "1.1.8"]
                 [crypto-password "0.1.3"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [studyflow/components "0.1.0-SNAPSHOT"]
                 [studyflow/migrations "0.1.0-SNAPSHOT"]
                 [org.bovinegenius/exploding-fish "0.3.4"]]

  :resource-paths ["../common/resources"]

  :profiles {:dev
             {:source-paths ["dev"]
              :resource-paths ["dev/resources"]
              :dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [org.clojure/tools.namespace "0.2.5"]
               [ring-mock "0.1.5"]]}
             :uberjar {:aot [studyflow.school-administration.main]
                       :main studyflow.school-administration.main}})
