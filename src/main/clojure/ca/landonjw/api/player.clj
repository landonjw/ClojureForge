(ns ca.landonjw.api.player
  (:require [ca.landonjw.api.inventory :as inventory])
  (:import (java.util UUID)
           (net.minecraft.entity.player ServerPlayerEntity)
           (net.minecraft.util.text StringTextComponent ITextComponent)
           (net.minecraft.world GameType)
           (net.minecraftforge.fml.server ServerLifecycleHooks)
           (net.minecraft.server MinecraftServer)
           (net.minecraft.server.management PlayerList)
           (net.minecraft.util.math.vector Vector3d)))

(defn ^ServerPlayerEntity from-name [name]
  (-> (^MinecraftServer ServerLifecycleHooks/getCurrentServer)
      (^PlayerList .getPlayerList)
      (.getPlayerByName ^String name)))

(defn ^PlayerList player-list []
  (-> (ServerLifecycleHooks/getCurrentServer)
      (.getPlayerList)))

(defn str->uuid [uuid]
  (UUID/fromString uuid))

(defn from-uuid [uuid]
  (cond
    (uuid? uuid) (.getPlayer (player-list) uuid)
    (string? uuid) (.getPlayer (player-list) (str->uuid uuid))
    :else nil))

(defn vec3d->vec [^Vector3d vec3d]
  [(.x vec3d) (.y vec3d) (.z vec3d)])

(defn game-type->keyword [^GameType game-type]
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

(defn ->clj [^ServerPlayerEntity player]
  (if player
    {:uuid       (-> player .getStringUUID)
     :name       (-> player .getName .getString)
     :health     (-> player .getHealth)
     :max-health (-> player .getMaxHealth)
     :position   (-> player .position vec3d->vec)
     :game-mode  (-> player .gameMode .getGameModeForPlayer game-type->keyword)
     :inventory  (-> player inventory/get-inventory)}))

(defn send-message [^ServerPlayerEntity player ^ITextComponent message]
  (cond
    (map? player) (send-message (from-uuid (:uuid player)) message)
    (string? message) (send-message player (StringTextComponent. message))
    (and (instance? ServerPlayerEntity player) (instance? StringTextComponent message))
    (.sendMessage player message (UUID/randomUUID))))

(defmulti apply-to-player! (fn [property _ _ _] (identity property)))

(defn update-player-prop! [property ^ServerPlayerEntity player player-data update-data]
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
  [_ ^ServerPlayerEntity player _ new]
  (.setHealth player new))

(defmethod apply-to-player! :position
  [_ ^ServerPlayerEntity player _ [x y z]]
  (.setPos player x y z))

(defmethod apply-to-player! :game-mode
  [_ ^ServerPlayerEntity player _ new]
  (.setGameMode player (keyword->game-type new)))

(defmethod apply-to-player! :inventory
  [_ ^ServerPlayerEntity player _ new]
  (inventory/set-inventory player new))

(defmethod apply-to-player! :default
  [_ _ _ _]
  nil)