(ns studyflow.components.redis-session-store-test
  (:require [clojure.test :refer [deftest]]
            [studyflow.components.redis-session-store :refer [redis-session-store]]
            [studyflow.components.session-store-test :as tests]))

(deftest create-session-test
  (tests/create-session-test (redis-session-store)))

(deftest delete-session-test
  (tests/delete-session-test (redis-session-store)))
