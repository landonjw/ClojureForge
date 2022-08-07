(ns ca.landonjw.api.collection)

(defn one-of? [types val]
  (not= nil (some #(= val %) types)))