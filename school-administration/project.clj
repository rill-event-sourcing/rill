(defproject school-administration "0.1.0-SNAPSHOT"
  :description "School administration"
  :url "http://studyflow.nl"

  :dependencies [[compojure "1.1.8"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]
                 [org.bovinegenius/exploding-fish "0.3.4"]]

  :plugins [[lein-ring "0.8.10"]]
  :ring {:init studyflow.school-administration.dev/bootstrap!
         :auto-reload? false
         :handler studyflow.school-administration.main/app :port 4000}
  :profiles {:dev {:dependencies [[ring-mock "0.1.5"]]}})
