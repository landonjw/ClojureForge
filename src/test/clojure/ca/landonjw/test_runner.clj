(ns ca.landonjw.test-runner
  (:require [clojure.test :refer :all]))

(defn run []
  (run-tests
    'ca.landonjw.api.nbt-test))