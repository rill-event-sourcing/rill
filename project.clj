(defproject rill/rill "0.2.0-RC4"
  :description "An Event Sourcing Toolkit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/rill-event-sourcing/rill"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [org.clojure/tools.logging "0.2.6"]
                 [prismatic/schema "0.2.2"]
                 [slingshot "0.10.3"]
                 [environ "0.5.0"]
                 [identifiers "1.1.0"]
                 [org.clojure/java.jdbc "0.3.4"]
                 [postgresql "9.1-901.jdbc4"]
                 [com.taoensso/nippy "2.6.3"]]
  :profiles {:dev {:plugins [[lein-repack "0.2.8"]]}}
  :repack [{:path "src"
            :levels 2}])

