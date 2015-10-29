(defproject rill/rill-bench "0.2.0-RC4"
  :description "Throughput tests for rill event store"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/rill-event-sourcing/rill"
  :aot [rill.bench.writes]
  :main rill.bench.writes
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [org.clojure/tools.logging "0.2.6"]
                 [rill/rill "0.2.0-RC4"]
                 [rill/rill-psql "0.2.0-RC4"]
                 [criterium "0.4.3"]])
