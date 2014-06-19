(defproject studyflow/learning "0.1.0-SNAPSHOT"
  :description "Event-sourced learning application"
  :url "http://studyflow.nl/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [environ "0.5.0"]
                 [slingshot "0.10.3"]
                 [prismatic/schema "0.2.2"]
                 [com.stuartsierra/component "0.2.1"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [clout-link "0.0.6"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [ring-mock "0.1.5"]
                 [ring/ring-devel "1.1.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [me.raynes/conch "0.7.0"]
                 [com.stuartsierra/component "0.2.1"]]
  :profiles {:dev
             {:source-paths ["dev"]
              :dependencies
              [[org.clojure/tools.trace "0.7.5"]]}}
  :test-paths ["test"]
  :aliases {"server" ["run" "-m" "studyflow.main"]
            "validate-course-material" ["run" "-m" "studyflow.cli.validate-course-material-json"]})


