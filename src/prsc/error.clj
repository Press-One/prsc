(ns prsc.error
  "error const define"
  )

(def ^:const PRS_CHAIN_SERVICE_ERROR 8001)
(def ^:const SIGN_ERROR 8002)
(def ^:const MIXIN_SERVICE_ERROR 8003)
(def ^:const UNSUPPORTED_PAYMENT_SERVICE_ERROR 8500)
(def ^:const PRS_DATA_ERROR 9001)
(def ^:const PRS_DATA_IS_NULL 9002)
(def ^:const BILL_DATA_ERROR 9003)
(def ^:const CONTRACT_DATA_ERROR 9004)
(def ^:const PRS_NOCONTRACT_ERROR 9005)
(def ^:const PRS_RUNCONTRACT_ERROR 9006)
(def ^:const TRACE_ID_NOT_MATCH_ERROR 9007)
(def ^:const RECEIPT_BENEFICIARY_ADDRESS_WALLET_NOT_MATCH_ERROR 9008)
(def ^:const RECEIPT_CONTRACT_PAYMENT_NOT_MATCH_ERROR 9009)

(defn provider-uid-name [provider]
  (cond
    (= "MIXIN" provider) "snapshot_id"
    :else "unique_id"
  )
)

(defn format-error-msg
  "error message formatting"
  [errcode opt]
  (cond
  (= errcode TRACE_ID_NOT_MATCH_ERROR) (str "trace_id not match: " (:l_name opt) "_trace_id:" (:l_trace_id opt) " " (:r_name opt) "_trace_id:" (:r_trace_id opt) " at " (:r_name opt) " " (provider-uid-name (:r_name opt)) ":" (:r_chain_id opt))
  (= errcode RECEIPT_BENEFICIARY_ADDRESS_WALLET_NOT_MATCH_ERROR) (str "receipt and beneficiary not match: beneficiary_address " (:beneficiary_address opt) " receipt_beneficiary_address " (:receipt_beneficiary_address opt) " beneficiary_wallet_id " (:beneficiary_wallet_id opt) " receipt_beneficiary_wallet_id "(:receipt_beneficiary_wallet_id opt))
  (= errcode RECEIPT_CONTRACT_PAYMENT_NOT_MATCH_ERROR) (str "receipt and contract not match: contract_address" (:contract_address opt) " receipt_beneficiary_address " (:receipt_beneficiary_address opt) " contract_price " (:contract_price opt) " receipt_amount " (:receipt_amount opt))
  (= errcode MIXIN_SERVICE_ERROR) (str "MIXIN network status code: " (:status opt) " URI: " (:uri opt))
  (= errcode PRS_CHAIN_SERVICE_ERROR) (str "PRS chain network status code:" (:status opt) " block_num: " (:block_num opt))
  (= errcode UNSUPPORTED_PAYMENT_SERVICE_ERROR) (str "provider name: " (:provider opt))
  (= errcode BILL_DATA_ERROR) (str "PRS bill_data block error with bill_id: " (:bill_id opt))
  (= errcode CONTRACT_DATA_ERROR) (str "PRS contract_data block error with contract_id: " (:contract_id opt))
  (= errcode PRS_NOCONTRACT_ERROR) (cond
                                      (some? (:block_num opt)) (str "PRS contract data not found at block_num: " (:block_num opt) " trx_id: " (:trx_id opt))
                                      (some? (:rID opt)) (str "PRS contract data not found at rID: " (:rID opt)) :else "unexpected error")
  (= errcode PRS_DATA_IS_NULL) (str "PRS DATA in the transaction node not found.")
  (= errcode PRS_DATA_ERROR) (str "PRS_DATA_ERROR")
  :else (str "unexpected error: " errcode))
)


(defn make-error
  "make the error object for API output"
  ([result errcode opt]
  {:result result :err errcode :msg (format-error-msg errcode opt)})
  ([result errcode]
  {:result result :err errcode :msg (format-error-msg errcode nil)})
)

(defn prserr? [obj]
  (if (nil? (:err obj))
    false
    true
  )
)
