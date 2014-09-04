(defproject studyflow/studyflow "0.1.0-SNAPSHOT"
  :dependencies [[compojure/compojure "1.1.8"]
                 [com.stuartsierra/component "0.2.1"]
                 [com.taoensso/carmine "2.6.2"]
                 [crypto-password/crypto-password "0.1.3"]
                 [environ/environ "0.5.0"]
                 [hiccup/hiccup "1.0.5"]
                 [nl.studyflow/eduroute-api "0.0.3"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [rill/rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [digest/digest "1.4.4"]
                 [clj-http/clj-http "0.9.1"]
                 [cheshire/cheshire "5.3.1"]
                 [enlive/enlive "1.1.5"]
                 [prismatic/schema "0.2.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [clout-link/clout-link "0.0.6"]
                 [ring-mock/ring-mock "0.1.5"]
                 [ring/ring-devel "1.2.1"]
                 [com.mindscapehq/raygun4java "1.5.0" :extension "pom"]
                 [com.mindscapehq/core "1.5.0"]
                 [identifiers/identifiers "1.0.0"]
                 [org.bovinegenius/exploding-fish "0.3.4"]]
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
             :dev                   {:resource-paths ["super-system/dev/resources"]
                                     :source-paths ["super-system/dev" "learning/dev" "login/dev"
                                                    "super-system/src"
                                                    "school-administration/dev" "teaching/dev"]
                                     :uberjar-name "dev.jar"
                                     :dependencies [[org.clojure/tools.trace "0.7.5"]
                                                    [org.clojure/tools.namespace "0.2.5"]
                                                    [org.clojure/clojurescript "0.0-2311"]
                                                    [om "0.7.1"]
                                                    [com.facebook/react "0.9.0.1"]
                                                    [cljs-ajax "0.2.3"]
                                                    [cljs-uuid "0.0.4"]]
                                     :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
                                               [com.cemerick/clojurescript.test "0.3.0"]]}}
  :cljsbuild {:builds {:dev {:source-paths ["learning/cljs/src"]
                             :compiler {:output-to "learning/generated/learning/public/js/studyflow-dev.js"
                                        :output-dir "learning/generated/learning/public/js/out"
                                        :source-map "learning/generated/learning/public/js/studyflow-dev.sourcemap"
                                        :optimizations :whitespace}}
                       :prod {:source-paths ["learning/cljs/src"]
                              :jar true
                              :compiler {:output-to "learning/generated/learning/public/js/studyflow.js"
                                         :optimizations :advanced
                                         :elide-asserts true
                                         :pretty-print false
                                         ;;;; handy debug options:
                                         ;; :pretty-print true
                                         ;; :print-input-delimiter true
                                         ;; :pseudo-names true
                                         :preamble ["react/react.min.js"]
                                         :externs ["react/externs/react.js"]}}}}

  :source-paths ["common/src"
                 "school-administration/src"
                 "teaching/src"
                 "learning/src"
                 "login/src"]
  :resource-paths ["learning/resources" "login/resources" "school-administration/resources" "teaching/resources"
                   "learning/generated" "login/generated" "school-administration/generated" "teaching/generated"
                   "learning/dev/resources" "login/dev/resources" "school-administration/dev/resources" "teaching/dev/resources"]
  :test-paths ["school-administration/test"
               "learning/test"
               "teaching/test"
               "common/test"
               "login/test"])
