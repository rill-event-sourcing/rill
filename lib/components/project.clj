(defproject studyflow/components "0.1.0-SNAPSHOT"
  :dependencies [[com.stuartsierra/component "0.2.1"]
                 [environ "0.5.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/tools.logging "0.3.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [com.taoensso/carmine "2.6.2"]
                 [com.mindscapehq/raygun4java "1.5.0" :extension "pom"]
                 [com.mindscapehq/core "1.5.0"]]
  :source-paths ["src" "../../common/src"])
