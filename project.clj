(defproject studyflow/studyflow "0.1.0-SNAPSHOT"
  :dependencies [[cheshire/cheshire "5.3.1"]
                 [clj-http/clj-http "0.9.1"]
                 [clj-redis-session "2.1.0"]
                 [clj-time/clj-time "0.8.0"]
                 [clojurewerkz/elastisch "2.1.0-beta4"]
                 [clout-link/clout-link "0.0.6" :exclusions [clout]]
                 [com.mindscapehq/core "1.5.0"]
                 [com.mindscapehq/raygun4java "1.5.0" :extension "pom"]
                 [com.stuartsierra/component "0.2.1"]
                 [com.taoensso/carmine "2.6.2"]
                 [crypto-password/crypto-password "0.1.3"]
                 [digest/digest "1.4.4"]
                 [dk.ative/docjure "1.6.0"]
                 [enlive/enlive "1.1.5"]
                 [environ/environ "0.5.0"]
                 [hiccup/hiccup "1.0.5"]
                 [nl.studyflow/eduroute-api "0.0.3"]
                 [org.bovinegenius/exploding-fish "0.3.4"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [prismatic/schema "0.2.2"]
                 [rill/rill "0.1.3"]
                 [ring-mock/ring-mock "0.1.5"]
                 [ring.middleware.logger "0.5.0"]
                 [ring/ring-defaults "0.1.0"]
                 [ring/ring-devel "1.2.1"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [ring/ring-json "0.3.1"]
                 [compojure/compojure "1.1.8"]]
  :profiles {:uberjar               {:aot :all
                                     :omit-source true}
             :learning              {:main studyflow.main
                                     :uberjar-name "learning-standalone.jar"}
             :school-administration {:main studyflow.school-administration.main
                                     :uberjar-name "school-administration-standalone.jar"}
             :login                 {:main studyflow.login.launcher
                                     :uberjar-name "login-standalone.jar"}
             :teaching              {:main studyflow.teaching.main
                                     :uberjar-name "teaching-standalone.jar"}
             :reporting             {:main studyflow.reporting.main
                                     :uberjar-name "reporting-standalone.jar"}
             :dev                   {:resource-paths ["dev/resources"]
                                     :source-paths ["dev"]
                                     :uberjar-name "dev.jar"
                                     :dependencies [[org.clojure/tools.trace "0.7.5"]
                                                    [org.clojure/tools.namespace "0.2.5"]
                                                    [org.clojure/clojurescript "0.0-2322"]
                                                    [om "0.7.3"]
                                                    [com.facebook/react "0.9.0.1"]
                                                    [cljs-ajax "0.2.3"]
                                                    [cljs-uuid "0.0.4"]]
                                     :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
                                               [com.cemerick/clojurescript.test "0.3.0"]]}}
  :cljsbuild {:builds {:dev {:source-paths ["cljs"]
                             :compiler {:output-to "generated/learning/public/js/studyflow-dev.js"
                                        :output-dir "generated/learning/public/js/out"
                                        :source-map "generated/learning/public/js/studyflow-dev.sourcemap"
                                        :optimizations :whitespace}}
                       :prod {:source-paths ["cljs"]
                              :jar true
                              :compiler {:output-to "generated/learning/public/js/studyflow.js"
                                         :optimizations :advanced
                                         :elide-asserts true
                                         :pretty-print false
                                         ;;;; handy debug options:
                                         ;; :pretty-print true
                                         ;; :print-input-delimiter true
                                         ;; :pseudo-names true
                                         :preamble ["react/react.min.js"]
                                         :externs ["react/externs/react.js"]}}}}

  :source-paths ["src"]
  :resource-paths ["resources" "generated"]
  :test-paths ["test"])
