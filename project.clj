(defproject rill/rill "0.1.10"
  :description "An Event Sourcing Toolkit"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/rill-event-sourcing/rill"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [org.clojure/tools.logging "0.2.6"]
                 [prismatic/schema "0.2.2"]
                 [slingshot "0.10.3"]
                 [environ "0.5.0"]
                 [identifiers "1.1.0"]])
