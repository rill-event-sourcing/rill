(defproject rill/rill-psql "0.2.0-RC4"
  :description "An Event Sourcing Toolkit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/rill-event-sourcing/rill"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [rill/rill "0.2.0-RC4"]
                 [org.clojure/java.jdbc "0.3.4"]
                 [postgresql "9.1-901.jdbc4"]
                 [com.taoensso/nippy "2.6.3"]])
