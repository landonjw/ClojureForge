(ns ca.landonjw.api.threading
  (:require [ca.landonjw.api.event :as events]))

(def queue (atom []))

(defn execute-synchronous [fn]
  (swap! queue conj fn))

; TODO: Make a macro for executing on game-thread

(events/subscribe! :server-tick
  (fn [_]
    (let [queued-fns @queue]
      (reset! queue [])
      (doseq [queued-fn queued-fns]
        (queued-fn)))))