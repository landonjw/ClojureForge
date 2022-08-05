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
        (is (= {:type :list
                :value [{:type :string :value "foo"} {:type :string :value "bar"}]}
               result)))))

  (testing "List NBT with nested List NBT"
    (let [nested-list (ListNBT.)
          nbt (ListNBT.)]
      (.add nbt 0 nested-list)
      (let [result (nbt->clj nbt)]
        (is (= {:type :list
                :value [{:type :list :value []}]}
               result)))))

  (testing "Compound NBT with primitive NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nbt (CompoundNBT.)]
      (.put nbt "test" dummy-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type :compound
                :value {:test {:type :string :value "foo"}}}
               result)))))

  (testing "Compound NBT with nested list NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nested-list-nbt (ListNBT.)
          nbt (CompoundNBT.)]
      (.add nested-list-nbt 0 dummy-nbt)
      (.put nbt "test" nested-list-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type :compound
                :value {:test {:type :list
                               :value [{:type :string :value "foo"}]}}}
               result)))))

  (testing "Compound NBT with nested compound NBT"
    (let [dummy-nbt (StringNBT/valueOf "foo")
          nested-compound-nbt (CompoundNBT.)
          nbt (CompoundNBT.)]
      (.put nested-compound-nbt "test" dummy-nbt)
      (.put nbt "nested" nested-compound-nbt)
      (let [result (nbt->clj nbt)]
        (is (= {:type :compound
                :value {:nested {:type :compound
                                 :value {:test {:type :string :value "foo"}}}}}
               result))))))

(deftest strip-types-test)                                  ; TODO