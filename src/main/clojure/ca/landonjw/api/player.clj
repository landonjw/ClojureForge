(ns ca.landonjw.api.player
  (:require [ca.landonjw.api.inventory :as inventory])
  (:import (net.minecraftforge.fml.server ServerLifecycleHooks)
           (java.util UUID)
           (net.minecraft.entity.player ServerPlayerEntity)
           (net.minecraft.util.text StringTextComponent)
           (net.minecraft.world GameType)))

(defn from-name [name]
  (->
    (ServerLifecycleHooks/getCurrentServer)
    (.getPlayerList)
    (.getPlayerByName name)))

(defn player-list []
  (-> (ServerLifecycleHooks/getCurrentServer)
      (.getPlayerList)))

(defn str->uuid [uuid]
  (UUID/fromString uuid))

(defn from-uuid [uuid]
  (cond
    (uuid? uuid) (.getPlayer (player-list) uuid)
    (string? uuid) (.getPlayer (player-list) (str->uuid uuid))
    :else nil))

(defn vec3d->vec [vec3d]
  [(.x vec3d) (.y vec3d) (.z vec3d)])

(defn game-type->keyword [game-type]
  (condp = game-type
    (GameType/NOT_SET) :not-set
    (GameType/SURVIVAL) :survival
    (GameType/CREATIVE) :creative
    (GameType/ADVENTURE) :adventure
    (GameType/SPECTATOR) :spectator
    :else :unknown))

(defn keyword->game-type [keyword]
  (condp = keyword
    :not-set (GameType/NOT_SET)
    :survival (GameType/SURVIVAL)
    :creative (GameType/CREATIVE)
    :adventure (GameType/ADVENTURE)
    :spectator (GameType/SPECTATOR)
    :else nil))

(defn player->clj [player]
  (if (and player (instance? ServerPlayerEntity player))
    {:uuid       (-> player .getStringUUID)
     :name       (-> player .getName .getString)
     :health     (-> player .getHealth)
     :max-health (-> player .getMaxHealth)
     :position   (-> player .position vec3d->vec)
     :game-mode  (-> player .gameMode .getGameModeForPlayer game-type->keyword)
     :inventory  (-> player inventory/get-inventory)}))

(defn entry-set [map]
  (into [] map))

(defn send-message [player message]
  (cond
    (map? player) (send-message (from-uuid (:uuid player)) message)
    (string? message) (send-message player (StringTextComponent. message))
    (and (instance? ServerPlayerEntity player) (instance? StringTextComponent message))
    (.sendMessage player message (UUID/randomUUID))))

(defmulti apply-to-player! (fn [property _ _ _] (identity property)))

(defn update-player-prop! [property player player-data update-data]
  (let [old-value (get player-data property)
        new-value (get update-data property)]
    (if (not= old-value new-value)
      (apply-to-player! property player old-value new-value))))

(defn update-player! [player-map]
  (let [player (from-name (:name player-map))
        player-data (player-map player)
        update-data player-map]
    (if player
      (doseq [property (keys update-data)]
        (update-player-prop! property player player-data update-data))))
  nil)

(defmethod apply-to-player! :health
  [_ player _ new]
  (.setHealth player new))

(defmethod apply-to-player! :position
  [_ player _ [x y z]]
  (.setPos player x y z))

(defmethod apply-to-player! :game-mode
  [_ player _ new]
  (.setGameMode player (keyword->game-type new)))

(defmethod apply-to-player! :inventory
  [_ player _ new]
  (inventory/set-inventory player new))

(defmethod apply-to-player! :default
  [_ _ _ _]
  nil)