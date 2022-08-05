(ns ca.landonjw.api.nbt)

(defn one-of? [types val]
  (not= nil (some #(= val %) types)))

(defn strip-types [nbt]
  (let [type (:type nbt)]
    (cond
      (= type :compound) (reduce-kv (fn [acc key val] (assoc acc key (strip-types val))) {} (:value nbt))
      (one-of? [:byte-array :list :int-array :long-array] type) (reduce (fn [acc val] (conj acc (strip-types val))) [] (:value nbt))
      :else (:value nbt))))

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