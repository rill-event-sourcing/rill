(defproject studyflow/learning "0.1.0-SNAPSHOT"
  :description "Event-sourced learning application"
  :url "http://studyflow.nl/"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [com.taoensso/carmine "2.6.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [environ "0.5.0"]
                 [enlive "1.1.5"]
                 [prismatic/schema "0.2.2"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [clout-link "0.0.6"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [ring-mock "0.1.5"]
                 [ring/ring-devel "1.2.1"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.stuartsierra/component "0.2.1"]
                 [identifiers "1.0.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [studyflow/migrations "0.1.0-SNAPSHOT"]
                 [studyflow/components "0.1.0-SNAPSHOT"]]
  :resource-paths ["resources" "generated"]
  :source-paths ["src" "../common/src"]
  :profiles {:dev
             {:source-paths ["dev"]
              :resource-paths ["dev/resources" "resources" "generated"]
              :dependencies
              [[org.clojure/tools.trace "0.7.5"]
               [org.clojure/tools.namespace "0.2.5"]
               [org.clojure/clojurescript "0.0-2311"]
               [om "0.7.1"]
               [com.facebook/react "0.9.0.1"]
               [cljs-ajax "0.2.3"]
               [cljs-uuid "0.0.4"]]
              :plugins
              [[lein-cljsbuild "1.0.4-SNAPSHOT"]
               [com.cemerick/clojurescript.test "0.3.0"]]}
             :uberjar {:aot [studyflow.main]
                       :main studyflow.main}}
  :test-paths ["test"]
  :aliases {"server" ["run" "-m" "studyflow.main"]
            "validate-course-material" ["run" "-m" "studyflow.cli.validate-course-material-json"]}
  :cljsbuild {:builds {:dev {:source-paths ["cljs/src"]
                             :compiler {:output-to "generated/learning/public/js/studyflow-dev.js"
                                        :output-dir "generated/learning/public/js/out"
                                        :source-map "generated/learning/public/js/studyflow-dev.sourcemap"
                                        :optimizations :whitespace}}
                       :prod {:source-paths ["cljs/src"]
                              :compiler {:output-to "generated/learning/public/js/studyflow.js"
                                         :optimizations :advanced
                                         :elide-asserts true
                                         :pretty-print false
                                         ;;;; handy debug options:
                                         ;; :pretty-print true
                                         ;; :print-input-delimiter true
                                         ;; :pseudo-names true
                                         :preamble ["react/react.min.js"]
                                         :externs ["react/externs/react.js"]}}}})
