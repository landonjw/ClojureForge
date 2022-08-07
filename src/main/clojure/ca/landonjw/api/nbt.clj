(ns ca.landonjw.api.nbt
  (:require [ca.landonjw.api.collection :refer [one-of?]])
  (:import (net.minecraft.nbt ByteNBT IntNBT LongNBT FloatNBT DoubleNBT ByteArrayNBT StringNBT ListNBT CompoundNBT IntArrayNBT LongArrayNBT ShortNBT)))

(def id-to-tag-type
  {1  :byte
   2  :short
   3  :int
   4  :long
   5  :float
   6  :double
   7  :byte-array
   8  :string
   9  :list
   10 :compound
   11 :int-array
   12 :long-array})

(defn- generalized-tag-type [tag]
  (cond
    (one-of? [:byte :short :int :long :float :double] tag) :number
    (one-of? [:byte-array :int-array :long-array] tag) :primitive-list
    (= tag :list) :generic-list
    (= tag :compound) :map
    (= tag :string) :string
    :else nil))

(defn strip-types [nbt]
  (let [generalized-type (generalized-tag-type (:type nbt))]
    (cond
      (= generalized-type :map) (reduce-kv (fn [acc key val] (assoc acc key (strip-types val))) {} (:value nbt))
      (= generalized-type :generic-list) (reduce (fn [acc val] (conj acc (strip-types val))) [] (:value nbt))
      :else (:value nbt))))

(defn get-nbt-tag-type [nbt]
  (-> nbt .getId id-to-tag-type))

(defn get-generalized-nbt-tag-type [nbt]
  (-> nbt .getId id-to-tag-type generalized-tag-type))

(defmulti parse-nbt get-generalized-nbt-tag-type)

(defn nbt->clj [nbt]
  (if (not (nil? nbt))
    (let [tag-type (get-nbt-tag-type nbt)]
      {:type tag-type :value (parse-nbt nbt)})
    nil))

(defmethod parse-nbt :map [nbt]
  (let [keys (.getAllKeys nbt)
        keys-keywordized (map keyword keys)
        values (map #(nbt->clj (.get nbt %)) keys)]
    (zipmap keys-keywordized values)))

(defmethod parse-nbt :number [nbt]
  (-> nbt .getAsNumber))

(defmethod parse-nbt :primitive-list [nbt]
  (into [] (map parse-nbt nbt)))

(defmethod parse-nbt :generic-list [nbt]
  (into [] (map nbt->clj nbt)))

(defmethod parse-nbt :string [nbt]
  (-> nbt .getAsString))

(defn- create-entry [key nbt]
  {(keyword key) (.get nbt key)})

(defn assoc-nbt [nbt key type value]
  (assoc nbt key {:type type :value value}))

(defn assoc-in-nbt [nbt keys type value]
  (assoc-in nbt keys {:type type :value value}))

(defn expand-keys [keys]
  (interleave (repeat (count keys) :value) keys))

(defn get-value
  ([nbt]
   (:value nbt))
  ([nbt keys]
   (let [expanded-keys (expand-keys keys)]
     (get-in nbt expanded-keys))))

; TODO: Add way to go from clj->nbt, with both a new nbt object and merging with an existing one

(declare clj->nbt)

(defn- byte-nbt [value]
  (ByteNBT/valueOf (byte value)))

(defn- short-nbt [value]
  (ShortNBT/valueOf (short value)))

(defn- int-nbt [value]
  (IntNBT/valueOf (int value)))

(defn- long-nbt [value]
  (LongNBT/valueOf (long value)))

(defn- float-nbt [value]
  (FloatNBT/valueOf (float value)))

(defn- double-nbt [value]
  (DoubleNBT/valueOf (double value)))

(defn- byte-array-nbt [value]
  (ByteArrayNBT. (byte-array (map byte value))))

(defn- string-nbt [value]
  (StringNBT/valueOf (str value)))

(defn- list-nbt [value]
  (let [nbt (ListNBT.)
        elements (map clj->nbt value)]
    (doseq [element elements]
      (.add nbt (-> nbt .size) element))
    nbt))

(defn- compound-nbt [value]
  (let [nbt (CompoundNBT.)]
    (doseq [key (keys value)]
      (.put nbt (name key) (clj->nbt (get value key))))
    nbt))

(defn- int-array-nbt [value]
  (IntArrayNBT. (int-array (map int value))))

(defn- long-array-nbt [value]
  (LongArrayNBT. (long-array (map long value))))

(defn clj->nbt [{type :type value :value}]
  (condp = type
    :byte (byte-nbt value)
    :short (short-nbt value)
    :int (int-nbt value)
    :long (long-nbt value)
    :float (float-nbt value)
    :double (double-nbt value)
    :byte-array (byte-array-nbt value)
    :string (string-nbt value)
    :list (list-nbt value)
    :compound (compound-nbt value)
    :int-array (int-array-nbt value)
    :long-array (long-array-nbt value)))