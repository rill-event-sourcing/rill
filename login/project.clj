(defproject studyflow/login "0.1.0-SNAPSHOT"
  :description "Authentication hub"
  :url "http://studyflow.nl/"

  :dependencies [[compojure "1.1.8"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-ring "0.8.10"]]

  :ring {:handler studyflow.login.main/app})
