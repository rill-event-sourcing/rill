(defproject studyflow/teaching "0.1.0-SNAPSHOT"
  :description "Teaching"

  :dependencies [[clj-time "0.8.0"]
                 [compojure "1.1.8"]
                 [dk.ative/docjure "1.6.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [studyflow/migrations "0.1.0-SNAPSHOT"]
                 [studyflow/components "0.1.0-SNAPSHOT"]]

  :source-paths ["src" "../common/src"]
  :resource-paths ["../common/resources" "resources" "generated"]

  :profiles {:test {:source-paths ["test"
                                   "../learning/src"
                                   "../school-administration/src"]}
             :dev {:source-paths ["dev"]
                   :resource-paths ["dev/resources" "resources" "generated"]
                   :dependencies
                   [[org.clojure/tools.trace "0.7.5"]
                    [org.clojure/tools.namespace "0.2.5"]
                    [enlive "1.1.5"]
                    [ring-mock "0.1.5"]]}
             :uberjar {:aot [studyflow.teaching.main]
                       :main studyflow.teaching.main}})
