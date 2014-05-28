(defproject studyflow/learning "0.1.0-SNAPSHOT"
  :description "Event-sourced learning application"
  :url "http://studyflow.nl/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [environ "0.5.0"]
                 [slingshot "0.10.3"]]
  :profiles {:dev
             {:dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [midje "1.6.3"]]}}
  :plugins [[cider/cider-nrepl "0.7.0-SNAPSHOT"]])
