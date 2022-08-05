(ns ca.landonjw.api.nbt-test
  (:require [clojure.test :refer :all]
            [ca.landonjw.api.nbt :refer :all])
  (:import (net.minecraft.nbt ByteNBT IntNBT ShortNBT LongNBT DoubleNBT FloatNBT ByteArrayNBT StringNBT IntArrayNBT LongArrayNBT ListNBT CompoundNBT)))

(defn within-delta? [a b]
  (let [delta 0.0001]
    (< (- a b) delta)))

(deftest nbt->clj-test
  (testing "Byte NBT"
    (let [value 1
          nbt (ByteNBT/valueOf (byte value))
          result (nbt->clj nbt)]
      (is (= {:type :byte :value value} result))))

  (testing "Short NBT"
    (let [value 123
          nbt (ShortNBT/valueOf value)
          result (nbt->clj nbt)]
      (is (= {:type :short :value value} result))))

  (testing "Integer NBT"
    (let [value 123
          nbt (IntNBT/valueOf value)
          result (nbt->clj nbt)]
      (is (= {:type :int :value value} result))))

  (testing "Long NBT"
    (let [value 123
          nbt (LongNBT/valueOf value)
          result (nbt->clj nbt)]
      (is (= {:type :long :value value} result))))

  (testing "Float NBT"
    (let [value 123.123
          nbt (FloatNBT/valueOf value)
          result (nbt->clj nbt)]
      (is (= :float (:type result)))
      (is (within-delta? value (:value result)))))

  (testing "Double NBT"
    (let [value 123.123
          nbt (DoubleNBT/valueOf 123.123)
          result (nbt->clj nbt)]
      (is (= :double (:type result)))
      (is (within-delta? value (:value result)))))

  (testing "String NBT"
    (let [value "foobar"
          nbt (StringNBT/valueOf value)
          result (nbt->clj nbt)]
      (is (= {:type :string :value value} result))))

  (testing "Empty byte array NBT"
    (let [value []
          nbt (ByteArrayNBT. (byte-array value))
          result (nbt->clj nbt)]
      (is (= {:type :byte-array :value value} result))))

  (testing "Byte array NBT"
    (let [value [1 0 1 0 1]
          nbt (ByteArrayNBT. (byte-array value))
          result (nbt->clj nbt)]
      (is (= {:type :byte-array :value value} result))))

  (testing "Empty int array NBT"
    (let [value []
          nbt (IntArrayNBT. (int-array value))
          result (nbt->clj nbt)]
      (is (= {:type :int-array :value value} result))))

  (testing "Int array NBT"
    (let [value [123 321 123]
          nbt (IntArrayNBT. (int-array value))
          result (nbt->clj nbt)]
      (is (= {:type :int-array :value value} result))))

  (testing "Empty long array NBT"
    (let [value []
          nbt (LongArrayNBT. (long-array value))
          result (nbt->clj nbt)]
      (is (= {:type :long-array :value value} result))))

  (testing "Long array NBT"
    (let [value [123 321 123]
          nbt (LongArrayNBT. (long-array value))
          result (nbt->clj nbt)]
      (is (= {:type :long-array :value value} result))))

  (testing "Empty list NBT"
    (let [nbt (ListNBT.)
          result (nbt->clj nbt)]
      (is (= {:type :list :value []} result))))

  (testing "List NBT with primitive NBT"
    (let [dummy-nbt-1 (StringNBT/valueOf "foo")
          dummy-nbt-2 (StringNBT/valueOf "bar")
          nbt (ListNBT.)]
      (.add nbt 0 dummy-nbt-1)
      (.add nbt 1 dummy-nbt-2)
      (let [result (nbt->clj nbt)]
        (is (= {:type  :list
                :value [{:type :string :value "foo"} {:type :string :value "bar"}]}
               result)))))

  (testing "List NBT with nested List NBT"
    (let [nested-list (ListNBT.)
          nbt (ListNBT.)]
      (.add nbt 0 nested-list)
      (let [result (nbt->clj nbt)]
        (is (= {:type  :list
                :value [{:type :list :value []}]}
               result)))))

  (testing "Compound NBT with primitive NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nbt (CompoundNBT.)]
      (.put nbt "test" dummy-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type  :compound
                :value {:test {:type :string :value "foo"}}}
               result)))))

  (testing "Compound NBT with nested list NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nested-list-nbt (ListNBT.)
          nbt (CompoundNBT.)]
      (.add nested-list-nbt 0 dummy-nbt)
      (.put nbt "test" nested-list-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type  :compound
                :value {:test {:type  :list
                               :value [{:type :string :value "foo"}]}}}
               result)))))

  (testing "Compound NBT with nested compound NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nested-compound-nbt (CompoundNBT.)
          nbt (CompoundNBT.)]
      (.put nested-compound-nbt "test" dummy-nbt)
      (.put nbt "nested" nested-compound-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type  :compound
                :value {:nested {:type  :compound
                                 :value {:test {:type :string :value "foo"}}}}}
               result))))))

(deftest strip-types-test
  (testing "Primitive NBTs return just their value"
    (are [nbt] (= (:value nbt) (strip-types nbt))
      {:type :byte :value 0}
      {:type :short :value 123}
      {:type :int :value 123}
      {:type :long :value 123}
      {:type :float :value 123.123}
      {:type :double :value 123.123}
      {:type :string :value "foobar"}))

  (testing "Primitive array NBTs return array of their values"
    (are [nbt] (= (:value nbt) (strip-types nbt))
      {:type :byte-array :value []}
      {:type :byte-array :value [1 0 1 0 1]}
      {:type :int-array :value []}
      {:type :int-array :value [123 321 123]}
      {:type :long-array :value []}
      {:type :long-array :value [123 321 123]}))

  (testing "List NBT returns expected value for primitives"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :list :value []} []
      {:type :list :value [{:type :string :value "foo"}]} ["foo"]
      {:type :list :value [{:type :int :value 123}]} [123]
      {:type :list :value [{:type :string :value "foo"} {:type :string :value "bar"}]} ["foo" "bar"]
      {:type :list :value [{:type :string :value "foo"} {:type :int :value 123}]} ["foo" 123]))

  (testing "List NBT returns expected value for primitive arrays"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :list :value [{:type :byte-array :value [1 0 1 0 1]}]} [[1 0 1 0 1]]
      {:type :list :value [{:type :int-array :value [123 321 123]}]} [[123 321 123]]
      {:type :list :value [{:type :long-array :value [123 321 123]}]} [[123 321 123]]))

  (testing "List NBT returns expected value for nested generic NBTs"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :list :value [{:type :compound :value {:foo {:type :string :value "bar"}}}]} [{:foo "bar"}]
      {:type :list :value [{:type :list :value [{:type :string :value "foo"}]}]} [["foo"]]))

  (testing "Compound NBT returns expected value for primitives"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :compound :value {}} {}
      {:type :compound :value {:foo {:type :string :value "bar"}}} {:foo "bar"}
      {:type :compound :value {:foo {:type :int :value 123} :bar {:type :int :value 321}}} {:foo 123 :bar 321}))

  (testing "Compound NBT returns expected value for primitive arrays"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :compound :value {:foo {:type :byte-array :value [1 0 1 0 1]}}} {:foo [1 0 1 0 1]}
      {:type :compound :value {:foo {:type :int-array :value [123 321 123]}}} {:foo [123 321 123]}
      {:type :compound :value {:foo {:type :long-array :value [123 321 123]}}} {:foo [123 321 123]}))

  (testing "Compound NBT returns expected value for nested generic NBTs"
    (are [nbt expected] (= (strip-types nbt) expected)
      {:type :compound :value {:foo {:type :compound :value {:bar {:type :string :value "baz"}}}}} {:foo {:bar "baz"}}
      {:type :compound :value {:foo {:type :list :value []}}} {:foo []}
      {:type :compound :value {:foo {:type :list :value [{:type :int :value 123}]}}} {:foo [123]})))

(deftest clj->nbt-test
  (testing "Byte NBT"
    (let [value 1
          clj-nbt {:type :byte :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ByteNBT result))
      (is (= value (.getAsByte result)))))

  (testing "Short NBT"
    (let [value 123
          clj-nbt {:type :short :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ShortNBT result))
      (is (= value (.getAsShort result)))))

  (testing "Integer NBT"
    (let [value 123
          clj-nbt {:type :int :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? IntNBT result))
      (is (= value (.getAsInt result)))))

  (testing "Long NBT"
    (let [value 123
          clj-nbt {:type :long :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? LongNBT result))
      (is (= value (.getAsLong result)))))

  (testing "Float NBT"
    (let [value 123.123
          clj-nbt {:type :float :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? FloatNBT result))
      (is (within-delta? value (.getAsFloat result)))))

  (testing "Double NBT"
    (let [value 123.123
          clj-nbt {:type :double :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? DoubleNBT result))
      (is (within-delta? value (.getAsDouble result)))))

  (testing "String NBT"
    (let [value "foobar"
          clj-nbt {:type :string :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? StringNBT result))
      (is (= value (.getAsString result)))))

  (testing "Empty byte array NBT"
    (let [value []
          clj-nbt {:type :byte-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ByteArrayNBT result))
      (is (= value (vec (.getAsByteArray result))))))

  (testing "Byte array NBT"
    (let [value [1 0 1 0 1]
          clj-nbt {:type :byte-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ByteArrayNBT result))
      (is (= value (vec (.getAsByteArray result))))))

  (testing "Empty int array NBT"
    (let [value []
          clj-nbt {:type :int-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? IntArrayNBT result))
      (is (= value (vec (.getAsIntArray result))))))

  (testing "Int array NBT"
    (let [value [123 321 123]
          clj-nbt {:type :int-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? IntArrayNBT result))
      (is (= value (vec (.getAsIntArray result))))))

  (testing "Empty long array NBT"
    (let [value []
          clj-nbt {:type :long-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? LongArrayNBT result))
      (is (= value (vec (.getAsLongArray result))))))

  (testing "Long array NBT"
    (let [value [123 321 123]
          clj-nbt {:type :long-array :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? LongArrayNBT result))
      (is (= value (vec (.getAsLongArray result))))))

  (testing "Empty list NBT"
    (let [value []
          clj-nbt {:type :list :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ListNBT result))
      (is (= (.size result) 0))))

  (testing "List NBT with primitive NBT"
    (let [dummy-nbt-1 {:type :string :value "foo"}
          dummy-nbt-2 {:type :string :value "bar"}
          value [dummy-nbt-1 dummy-nbt-2]
          clj-nbt {:type :list :value value}
          result (clj->nbt clj-nbt)]
      (is (instance? ListNBT result))
      (is (.size result) 2)
      (let [element-1 (.get result 0)
            element-2 (.get result 1)]
        (is (instance? StringNBT element-1))
        (is (= (.getAsString element-1) "foo"))
        (is (instance? StringNBT element-2))
        (is (= (.getAsString element-2) "bar")))))

  (testing "List NBT with nested List NBT"
    (let [nested-list {:type :list :value []}
          clj-nbt {:type :list :value [nested-list]}
          result (clj->nbt clj-nbt)]
      (is (instance? ListNBT result))
      (is (.size result) 1)
      (let [element (.get result 0)]
        (is (instance? ListNBT element))
        (is (= (.size element) 0)))))

  (testing "Compound NBT with primitive NBT"
    (let [dummy-nbt {:type :string :value "foo"}
          clj-nbt {:type :compound :value {:test dummy-nbt}}
          result (clj->nbt clj-nbt)]
      (is (instance? CompoundNBT result))
      (is (= (.size result) 1))
      (let [element (.get result "test")]
        (is (instance? StringNBT element))
        (is (= (.getAsString element) "foo")))))

  (testing "Compound NBT with nested list NBT"
    (let [dummy-nbt {:type :string :value "foo"}
          nested-list-nbt {:type :list :value [dummy-nbt]}
          clj-nbt {:type :compound :value {:test nested-list-nbt}}
          result (clj->nbt clj-nbt)]
      (is (instance? CompoundNBT result))
      (is (= (.size result) 1))
      (let [nested-list (.get result "test")]
        (is (instance? ListNBT nested-list))
        (is (= (.size nested-list) 1))
        (let [nested-string (.get nested-list 0)]
          (is (instance? StringNBT nested-string))
          (is (= (.getAsString nested-string) "foo"))))))

  (testing "Compound NBT with nested compound NBT"
    (let [dummy-nbt {:type :string :value "foo"}
          nested-compound-nbt {:type :compound :value {:test dummy-nbt}}
          clj-nbt {:type :compound :value {:nested nested-compound-nbt}}
          result (clj->nbt clj-nbt)]
      (is (instance? CompoundNBT result))
      (is (= (.size result) 1))
      (let [nested-compound (.get result "nested")]
        (is (instance? CompoundNBT nested-compound))
        (is (= (.size nested-compound) 1))
        (let [nested-string (.get nested-compound "test")]
          (is (instance? StringNBT nested-string))
          (is (= (.getAsString nested-string) "foo")))))))