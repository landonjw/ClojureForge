(ns ca.landonjw.api.item-stack
  (:import (net.minecraft.item ItemStack)
           (net.minecraft.util ResourceLocation)
           (net.minecraftforge.registries ForgeRegistries)
           (net.minecraft.util.text StringTextComponent))
  (:require [clojure.string :refer [split]]
            [ca.landonjw.api.nbt :as nbt]
            [clojure.data :as data]))

(defn in-map? [maybe-submap map]
  (let [[exclusive-in-submap _ _] (data/diff maybe-submap map)]
    (= exclusive-in-submap nil)))

(defn- resource-location->keyword [resource-location]
  (let [namespace (.getNamespace resource-location)
        path (.getPath resource-location)]
    (keyword (str namespace "/" path))))

(defn keyword->resource-location [keyword]
  (let [split (-> (str keyword) (subs 1) (split #"/"))
        namespace (first split)
        path (second split)]
    (ResourceLocation. namespace path)))

; TODO: Implement properly
(defn- get-lore-for-item-stack [item-stack]
  [])

(defn item-stack->map [item-stack]
  (if (instance? ItemStack item-stack)
    (if (.isEmpty item-stack)
      nil
      {:item         (-> item-stack .getItem .getRegistryName resource-location->keyword)
       :amount       (-> item-stack .getCount)
       :display-name (-> item-stack .getHoverName .getString)
       :nbt          (-> item-stack .getTag nbt/nbt->clj)
       :lore         (-> item-stack get-lore-for-item-stack)})))

(defmulti apply-to-item-stack! (fn [property _ _ _] (identity property)))

(defmethod apply-to-item-stack! :amount
  [_ item-stack _ new]
  (.setCount item-stack new))

(defmethod apply-to-item-stack! :display-name
  [_ item-stack _ new]
  (.setHoverName item-stack (StringTextComponent. new)))

; TODO
(defmethod apply-to-item-stack! :lore
  [_ item-stack _ new]
  nil)

; TODO
(defmethod apply-to-item-stack! :nbt
  [_ item-stack old new]
  nil)

(defmethod apply-to-item-stack! :default
  [_ _ _ _]
  nil)

(defn update-item-stack-prop! [property item-stack old-data update-data]
  (let [old-value (get old-data property)
        new-value (get update-data property)]
    (if (not= old-value new-value)
      (apply-to-item-stack! property item-stack old-value new-value))))

(defn update-item-stack! [item-stack item-stack-map]
  (let [old-data (item-stack->map item-stack)
        update-data item-stack-map]
    (doseq [property (keys update-data)]
      (update-item-stack-prop! property item-stack old-data update-data))))

(defn create-item-stack [map]
  (if (nil? (:item map))
    (ItemStack/EMPTY)
    (let [item-resource-loc (keyword->resource-location (:item map))
          item (.getValue (ForgeRegistries/ITEMS) item-resource-loc)]
      (if item
        (let [item-stack (ItemStack. item)]
          (update-item-stack! item-stack map)
          item-stack)))))