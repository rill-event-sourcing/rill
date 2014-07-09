(defproject school-administration "0.1.0-SNAPSHOT"
  :description "School administration"
  :url "http://studyflow.nl"
  
  :dependencies [[compojure "1.1.8"]
                 [environ "0.5.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.3"]
                 [org.clojure/tools.logging "0.3.0"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
                 [rill "0.1.0-SNAPSHOT"]
                 [ring/ring-defaults "0.1.0"]
                 [ring-server "0.3.1"]]

  :plugins [[lein-ring "0.8.10"]]
  :ring {;;:init studyflow.school-administration.main/bootstrap!
         :handler studyflow.school-administration.main/app :port 4000}
  :aliases {"prepare-database" ["run" "-m" "studyflow.school-administration.prepare-database"]}

  :profiles {:dev {:dependencies [[enlive "1.1.5"]
                                  [ring-mock "0.1.5"]]}}
  
)
