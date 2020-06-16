(ns prsc.core-test
  (:require [clojure.test :refer :all]
            [clara.rules :refer :all]
            [prsc.dsl :refer :all]
            [prsc.utils :refer :all]
            ))
(defn loadprsc []
  (slurp  "./doc/example.prsc"))

(deftest get-license-test
  (let [contractcode (loadprsc)]
    (let [session (-> (mk-session 'prsc.dsl (load-user-rules contractcode))
                      (insert (->license-type "Commercial"))
                      (fire-rules))]
      (let [result (query session get-license)]
        (testing "Contract Receiver:"
          (is (= (get-in (first result) [:?receiver :address]) "becd34540fefeab83730ffb479e98ee12fa1337e")))
        (testing "Contract Type"
          (is (= (get-in (first result) [:?license :type]) "Commercial")))
        (testing "Contract Currency"
          (is (= (get-in (first result) [:?license :currency]) "PRS")))
        (testing "Contract price"
          (is (= (get-in (first result) [:?license :price]) "0.0035"))))

)))
