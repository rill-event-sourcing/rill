(ns studyflow.components.temp-session-store-test
  (:require [clojure.test :refer [deftest]]
            [studyflow.components.temp-session-store :refer [temp-session-store]]
            [studyflow.components.session-store-test :as tests]))

(deftest create-session-test
  (tests/create-session-test (temp-session-store)))

(deftest delete-session-test
  (tests/delete-session-test (temp-session-store)))

