(defproject studyflow/super-system "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [studyflow/login "0.1.0-SNAPSHOT"]
                 [studyflow/learning "0.1.0-SNAPSHOT"]
                 [studyflow/school-administration "0.1.0-SNAPSHOT"]
                 [com.stuartsierra/component "0.2.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]]
  :profiles {:dev
             {:source-paths ["dev"]
              :resource-paths ["dev/resources"]
              :dependencies
              ~(let [find-dev-deps (fn [project-clj-file]
                                     (let [conf (->>
                                                 project-clj-file
                                                 slurp
                                                 read-string
                                                 (drop 3)
                                                 (partition 2)
                                                 (map vec)
                                                 (into {}))]
                                       (get-in conf [:profiles :dev :dependencies])))
                     dev-deps (->> (for [f (file-seq (java.io.File. "checkouts"))
                                         :let [n (.getPath f)]
                                         :when (.endsWith n "project.clj")]
                                     (find-dev-deps n))
                                   (reduce into []))]
                 (into '[[org.clojure/tools.trace "0.7.5"]
                         [org.clojure/tools.namespace "0.2.5"]]
                       dev-deps))}})
