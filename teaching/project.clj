(defproject studyflow/teaching "0.1.0-SNAPSHOT"
  :description "Teaching"

  :dependencies [[compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [studyflow/components "0.1.0-SNAPSHOT"]]

  :profiles {:dev
             {:source-paths ["dev"]
              :resource-paths ["dev/resources"]
              :dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [org.clojure/tools.namespace "0.2.5"]
               [ring-mock "0.1.5"]]}
             :uberjar {:aot [studyflow.teaching.main]
                       :main studyflow.teaching.main}})
