(ns ca.landonjw.api.logging
  (:import (org.apache.logging.log4j LogManager)))

(def logger (LogManager/getLogger "ClojureForge"))

(defn info! [message]
  (.info logger message))

(defn warn! [message]
  (.warn logger message))

(defn error! [message]
  (.error logger message))