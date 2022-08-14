(ns ca.landonjw.api.inventory
  (:require [ca.landonjw.api.item-stack :refer [item-stack->clj ->item-stack]])
  (:import (net.minecraft.entity.player ServerPlayerEntity)))

(defn slot->item-stack [^ServerPlayerEntity player slot]
  (let [inventory (-> player .inventory)]
    (-> (.getItem inventory slot)
        (item-stack->clj))))

(defn set-slot [^ServerPlayerEntity player slot item-stack]
  (let [inventory (-> player .inventory)]
    (.setItem inventory slot (->item-stack item-stack))))

(defn get-hot-bar [player]
  (->> (range 0 9)
       (map #(slot->item-stack player %))
       (into [])))

(defn set-hot-bar [player hot-bar]
  (->> (range 0 9)
       (run! #(set-slot player % (nth hot-bar %)))))

(defn get-storage [player]
  (->> (range 9 36)
       (map #(slot->item-stack player %))
       (into [])))

(defn set-storage [player storage]
  (->> (range 9 36)
       (run! #(set-slot player % (nth storage (- % 9))))))

(def armor-slots {:head 39
                  :chest 38
                  :legs 37
                  :boots 36})

(defn get-armor [player]
  {:head  (slot->item-stack player (:head armor-slots))
   :chest (slot->item-stack player (:chest armor-slots))
   :legs  (slot->item-stack player (:legs armor-slots))
   :boots (slot->item-stack player (:boots armor-slots))})

(defn set-armor [player armor]
  (doseq [armor-piece [:head :chest :legs :boots]]
    (set-slot player (get armor-slots armor-piece) (get armor armor-piece))))

(defn get-off-hand [player]
  (slot->item-stack player 40))

(defn set-off-hand [player item-stack]
  (set-slot player 40 item-stack))

(defn get-selected [^ServerPlayerEntity player]
  (-> player .inventory .selected))

(defn get-inventory [^ServerPlayerEntity player]
  {:selected-hot-bar-index (get-selected player)
   :hot-bar (get-hot-bar player)
   :storage (get-storage player)
   :armor   (get-armor player)
   :off-hand (get-off-hand player)})

(defn set-inventory [player inventory]
  (set-hot-bar player (:hot-bar inventory))
  (set-off-hand player (:off-hand inventory))
  (set-armor player (:armor inventory))
  (set-storage player (:storage inventory)))