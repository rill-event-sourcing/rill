(defproject studyflow/studyflow "0.1.0-SNAPSHOT"
  :dependencies [[cider/cider-nrepl "0.8.0-SNAPSHOT"]
                 [compojure/compojure "1.1.8"]
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
  :source-paths ["lib/migrations/src"
                 "lib/components/src"
                 "super-system/src"
                 "school-administration/src"
                 "teaching/src"
                 "learning/src"
                 "login/src"]
  :resource-paths ["learning/resources" "login/resources" "school-administration/resources" "teaching/resources"
                   "learning/generated" "login/generated" "school-administration/generated" "teaching/generated"
                   "learning/dev/resources" "login/dev/resources" "school-administration/dev/resources" "teaching/dev/resources"]
  :test-paths ["lib/components/test"
               "school-administration/test"
               "learning/test"
               "teaching/test"
               "login/test"]
  :profiles {:dev {:source-paths ["super-system/dev" "learning/dev" "login/dev" "school-administration/dev" "teaching/dev"]
                   :resource-paths ["super-system/dev/resources"]}})
