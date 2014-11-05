(ns cljs.user
  (:require [rekenmachien.core :as core]
            [rekenmachien.core-test :as core-test]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(defn ^:export app []
  (figwheel/watch-and-reload
   :websocket-url "ws://localhost:3449/figwheel-ws"
   :jsload-callback (fn [] (core/main)))
  (core/main))

(defn ^:export test []
  (figwheel/watch-and-reload
   :websocket-url "ws://localhost:3449/figwheel-ws"
   :jsload-callback (fn [] (core-test/run)))
  (core-test/run))
