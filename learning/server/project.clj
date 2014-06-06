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
                 [ring.middleware.logger "0.4.0"]
                 [ring-mock "0.1.5"]]
  :profiles {:dev
             {:dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [speclj "3.0.0"]]}}
  :plugins [[cider/cider-nrepl "0.7.0-SNAPSHOT"]
            [lein-ring "0.8.10"]
            [speclj "3.0.0"]]
  :test-paths ["test"]
  :ring {:handler studyflow.system/web-handler})

