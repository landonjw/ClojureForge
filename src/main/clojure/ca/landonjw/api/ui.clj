(ns ca.landonjw.api.ui
  (:require [ca.landonjw.api.item-stack :as item-stack]
            [clojure.core.memoize :as memoize]
            [ca.landonjw.api.text :as text])
  (:import (net.minecraft.item ItemStack)
           (net.minecraft.inventory.container Container ContainerType ClickType Slot)
           (net.minecraft.inventory IInventory)
           (net.minecraft.network.play.server SOpenWindowPacket SSetSlotPacket)
           (net.minecraftforge.fml.server ServerLifecycleHooks)
           (net.minecraft.entity.player ServerPlayerEntity)
           (net.minecraft.util.text StringTextComponent)
           (ca.landonjw UIContainer)))

(def create-item (memoize/ttl item-stack/->item-stack :ttl/threshold 300000))

(def page-type-mappings
  {:chest-9x1 (ContainerType/GENERIC_9x1)
   :chest-9x2 (ContainerType/GENERIC_9x2)
   :chest-9x3 (ContainerType/GENERIC_9x3)
   :chest-9x4 (ContainerType/GENERIC_9x4)
   :chest-9x5 (ContainerType/GENERIC_9x5)
   :chest-9x6 (ContainerType/GENERIC_9x6)
   :furnace   (ContainerType/FURNACE)})

(defn flatten-click-type [^ClickType click-type drag-type]
  (condp = click-type
    (ClickType/PICKUP) (if (= drag-type 0) :left-click :right-click)
    (ClickType/CLONE) :middle-click
    (ClickType/QUICK_MOVE) (if (= drag-type 0) :shift-left-click :shift-right-click)
    (ClickType/THROW) :throw
    :unknown))

(defn container-click-event [{:keys [slot click-type ^ServerPlayerEntity player page]}]
  (let [buttons (:buttons page)
        clicked-button (get buttons slot)
        clicked-button-callback (:on-click clicked-button)]
    (when clicked-button-callback
      (clicked-button-callback {:player     player
                                :click-type click-type
                                :page       page
                                :button     clicked-button}))))

(defn get-page-size [page-type]
  (condp = page-type
    :chest-9x1 (* 9 1)
    :chest-9x2 (* 9 2)
    :chest-9x3 (* 9 3)
    :chest-9x4 (* 9 4)
    :chest-9x5 (* 9 5)
    :chest-9x6 (* 9 6)))

(defn ^IInventory create-inventory-proxy [page]
  (proxy [IInventory] []
    (getContainerSize [] (get-page-size (:page-type page)))
    (isEmpty [] false)
    (getItem [index]
      (let [button (get (:buttons page) index)]
        (if button
          (create-item (:display button))
          (ItemStack/EMPTY))))
    (removeItem [_ _] (ItemStack/EMPTY))
    (removeItemNoUpdate [_] (ItemStack/EMPTY))
    (setItem [] nil)
    (setChanged [] nil)
    (stillValid [_] false)
    (clearContent [] nil)))

(defmulti container-click
          (fn [_ drag-type ^ClickType click-type _ _]
            (identity (flatten-click-type click-type drag-type))))

(defn clear-player-cursor! [^ServerPlayerEntity player]
  (let [packet (SSetSlotPacket. -1 0 (ItemStack/EMPTY))]
    (-> player .connection (.send packet))))

(defmethod container-click :middle-click [_ _ _ ^ServerPlayerEntity player _]
  (clear-player-cursor! player)
  (ItemStack/EMPTY))

(defmethod container-click :default [_ _ _ _ _]
  (ItemStack/EMPTY))

(declare render)

(defn update-container-contents [^Container container ^ServerPlayerEntity player]
  (let [inventory-menu (.inventoryMenu player)]
    (-> player (.refreshContainer container (.getItems container)))
    (-> inventory-menu .broadcastChanges)
    (-> player (.refreshContainer inventory-menu (.getItems inventory-menu)))))

(defn create-container-proxy [page ^ServerPlayerEntity player]
  (let [container-type (get page-type-mappings (:page-type page))
        ^IInventory inventory (create-inventory-proxy page)
        last-click-time (atom 0)
        ^UIContainer container (proxy [UIContainer] [container-type]

                               (stillValid [_] true)

                               (clicked [slot dragType clickType _]
                                 (let [server-time (.getTickCount (ServerLifecycleHooks/getCurrentServer))]
                                   (when (> (abs (- server-time @last-click-time)) 2)
                                     (do
                                       (container-click-event {:slot       slot
                                                               :click-type (flatten-click-type clickType dragType)
                                                               :player     player
                                                               :page       page})
                                       (reset! last-click-time server-time)))
                                   (update-container-contents this player)
                                   (container-click slot dragType clickType player page)))

                               (removed [_]
                                 (when-let [page-close-callback (:on-close page)]
                                   (page-close-callback {:page page :player player}))))]

    (doseq [index (range (get-page-size (:page-type page)))]
      (.addNewSlot container (Slot. inventory index 0 0)))
    (doseq [index (range 27)]
      (.addNewSlot container (Slot. (.inventory player) (+ 9 index) 0 0)))
    (doseq [index (range 9)]
      (.addNewSlot container (Slot. (.inventory player) index 0 0)))
    container))

(defn send-window-packet [page ^ServerPlayerEntity player]
  (let [container-counter (.containerCounter player)
        ^ContainerType container-type (get page-type-mappings (:page-type page))
        ^StringTextComponent title (text/->text-component (get page :title []))
        packet (SOpenWindowPacket. container-counter container-type title)]
    (-> player .connection (.send packet))))

(defn render [page ^ServerPlayerEntity player]
  (let [^Container container (create-container-proxy page player)]
    (.closeContainer player)
    (set! (.containerMenu player) container)
    (set! (.containerCounter player) (.containerId container))
    (send-window-packet page player)
    (when-let [page-open-callback (:on-open page)]
      (page-open-callback {:page page :player player}))
    (update-container-contents container player)))