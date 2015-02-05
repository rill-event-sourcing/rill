(defproject rill/rill-psql "0.1.8-SNAPSHOT"
  :description "An Event Sourcing Toolkit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/rill-event-sourcing/rill"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [rill/rill "0.1.8-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.3.4"]
                 [postgresql "9.1-901.jdbc4"]
                 [com.taoensso/nippy "2.6.3"]
                 [com.mchange/c3p0 "0.9.2.1"]])
