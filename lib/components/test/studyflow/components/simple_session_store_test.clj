(ns studyflow.components.simple-session-store-test
  (:require [clojure.test :refer [deftest]]
            [studyflow.components.simple-session-store :refer [simple-session-store]]
            [studyflow.components.session-store-test :as tests]))

(deftest create-session-test
  (tests/create-session-test (simple-session-store)))

(deftest delete-session-test
  (tests/delete-session-test (simple-session-store)))

