(ns ca.landonjw.bootstrap)

(defn start-nrepl-server []
  (let [port 7888]
       (println "Launching nREPL server on port " port)
       (@(requiring-resolve 'nrepl.server/start-server) :port port)))