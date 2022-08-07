(ns ca.landonjw.test-runner
  (:require [clojure.test :refer :all]))

(def unit-tests
  ['ca.landonjw.api.nbt-test])

(def integration-tests
  [])

(defn run [namespaces]
  (apply run-tests namespaces))

(defn start [opts]
  (condp = (:test-type opts)
    :unit (run unit-tests)
    :integration (run integration-tests)
    :all (run (into unit-tests integration-tests))))