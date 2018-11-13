(ns prsc.core-test
  (:require [clojure.test :refer :all]
            [clara.rules :refer :all]
            [prsc.core :refer :all]))
(defn loadprsc []
  (slurp  "./examples/example.prsc")
)

(deftest get-license-test
  (let [contractcode (loadprsc)]
    (let [session (-> (mk-session 'prsc.core (load-user-rules contractcode))
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
                  (is (= (get-in (first result) [:?license :price]) "0.0035")))
            )
  ))
  )
