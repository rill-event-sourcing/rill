(defproject studyflow/login "0.1.0-SNAPSHOT"
  :description "Authentication hub"
  :url "http://studyflow.nl/"

  :dependencies [[compojure "1.1.8"]
                 [com.taoensso/carmine "2.6.2"]
                 [crypto-password "0.1.3"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/tools.logging "0.3.0"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-ring "0.8.10"]]

  :ring {:init studyflow.login.system/bootstrap!
         :handler studyflow.login.main/app
         :port 4000}

  :profiles {:dev {:dependencies [[enlive "1.1.5"]
                                  [ring-mock "0.1.5"]]}}

  :main studyflow.login.launcher)
