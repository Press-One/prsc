(ns prsc.verify-test
  (:require [clojure.test :refer :all]
            [clara.rules :refer :all]
            [prsc.dsl :refer :all]
            [prsc.eos :refer :all]
            [prsc.error :refer :all]
            [prsc.verify :refer :all]
            [prsc.utils :refer :all]
            ))
(deftest verify-test
  (let [tx (prsc.eos/get-eos-tx 1472676 "0503EA4A4061A7142992AFCFD04DB083E563C5C103E9989E3FBF7B6E89D3F01B")]
    (is (= "0503ea4a4061a7142992afcfd04db083e563c5c103e9989e3fbf7b6e89d3f01b" (:id (:trx (prsc.utils/safenth (:tx tx) 0)))))
      (let [data (prsc.eos/verifytrx tx)]
       (is (= (:type data) "RECEIPT:2"))
        (let [prsdata (prsc.verify/verify-PRSdata data)]
          (is (= "cd5e8aa1-87df-408d-a5de-28c22e4f2b4c" (:payment_snapshot_id (:data prsdata))))
          (let [result (prsc.verify/parse-PRSData prsdata)]
            (is (= true (:result result)))
          )
        )
      )
  )
)
