(defproject studyflow/login "0.1.0-SNAPSHOT"
  :description "Authentication hub"
  :url "http://studyflow.nl/"

  :dependencies [[compojure "1.1.8"]
                 [crypto-password "0.1.3"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.clojure/tools.logging "0.3.0"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-ring "0.8.10"]]
  :ring {:init studyflow.login.main/bootstrap!
         :handler studyflow.login.main/app}
  :aliases {"prepare-database" ["run" "-m" "studyflow.login.prepare-database"]}

  :profiles {:dev {:dependencies [[enlive "1.1.5"]]}}
  
  :main studyflow.login.launcher)
