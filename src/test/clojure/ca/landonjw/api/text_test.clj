(ns ca.landonjw.api.text-test
  (:require [clojure.test :refer :all]
            [ca.landonjw.api.text :as text]))

(defn get-text-component-text [text-component]
  (let [contents (-> text-component .getContents)
        siblings (-> text-component .getSiblings)]
    (if (.isEmpty siblings)
      contents
      (str contents (reduce str (map get-text-component-text siblings))))))

(deftest ->test-component-test
  (testing "Content in text resulting text component is same as in vector"
    (are [text expected]
      (= expected (-> text text/->text-component get-text-component-text))
      ["foo"] "foo"
      ["foo" "bar"] "foobar"
      [:red "foo"] "foo"
      [["foo"]] "foo"
      [[:red "foo"]] "foo"
      [[:red "foo"] [:blue "bar"]] "foobar"
      ["foo" [:red "bar"] "baz"] "foobarbaz"
      [[:red "a"] [:green "b"] [:blue "c"] "d"] "abcd"
      [:red "a" [:green "b" [:blue "c" [:yellow "d"]]] "e"] "abcde")))