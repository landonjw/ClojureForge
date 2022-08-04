(ns ca.landonjw.api.event
  (:import (java.util UUID)
           (net.minecraftforge.event TickEvent$ServerTickEvent)
           (net.minecraftforge.event.entity.player PlayerEvent$PlayerLoggedInEvent)))

(def events {PlayerEvent$PlayerLoggedInEvent :player-logged-in
             TickEvent$ServerTickEvent       :server-tick})

(def event-listeners (atom {}))

(defn subscribe! [event-type action]
  (let [subscription-uuid (UUID/randomUUID)]
    (swap! event-listeners assoc-in [event-type subscription-uuid] action)
    {:event-type event-type :uuid subscription-uuid}))

(defn unsubscribe! [subscription]
  (swap! event-listeners #(dissoc (get % (:event-type subscription)) (:uuid subscription)))
  nil)

(defn post [event-class event]
  (let [event-listeners @event-listeners
        event-type (get events event-class)
        listeners-for-event (get event-listeners event-type)]
    (if listeners-for-event
      (doseq [listener (vals listeners-for-event)]
        (listener event)))))
