(ns prsc.verify
  "verify functions"
  (:require [config.core :refer [env]]
            [cheshire.core :refer :all]
            [instaparse.core :as insta]
            [clara.rules :refer :all]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [prsc.dsl :refer :all]
            [prsc.mixin :refer :all]
            [prsc.assetmapping :refer :all]
            [prsc.error :refer :all]))

(def enabled-input-fields ["license-type"])

(defn fieldmapping [field]
  (let [value ((keyword field)
               {:license "license-type"})]
    (if (nil? value)
      field
      value)))

(defn verify-PRSdata [data]
  (if (= true (or (nil? (:meta data)) (nil?  (:data data))))
    nil ;TOFIX error format
    {:type (:type data) :meta (parse-string (:meta data) true) :data (parse-string (:data data) true)}))

(defn fetch-PRSC [data]
  (let [user_address (:user_address data)
        meta (:meta data)]
    (let [undecoded_script (prsc.utils/safenth (:uris (parse-string meta true)) 0)]
      (let [start (clojure.string/index-of undecoded_script "PRSC")]
        (java.net.URLDecoder/decode (subs undecoded_script start))))))

(defn parse-get-license [result]
	(let [address (:address (:?receiver (first result)))
			currency (:currency (:?license (first result)))
			price (:price (:?license (first result)))
		 ]
		{:address address :currency currency :price price}
	)
)
(defn parseQueryResult [query-name result]
         (cond
           (= (name query-name) "get-license") (parse-get-license result) 
           :else {})
)

(defn block-prsid-mapping [prsid blockreferences]
    (let [block ((keyword prsid) blockreferences)]
      (let [block_num  (:block_num block)
            trx_id (:transaction_id block)]
        {:block_num block_num  :trx_id  trx_id}
      )
    )
)

;TOFIX unknown error...
(defn txresult-to-data [tx]
    (let [data (prsc.eos/verifytrx tx)]
      (if (nil? data)
        (make-error nil PRS_DATA_IS_NULL)
        (let [prsdata (verify-PRSdata data)]
          (if (nil? prsdata)
            (make-error nil PRS_DATA_ERROR)
            prsdata)))
    )
)

(defn tx-to-bill [tx]
  (let [data (txresult-to-data tx)]  
    (if (nil? (:err data))
      (if (= "BILL:2" (:type data))
      		data
      		{}	
      )
      data
    )
	
  )
)

(defn verify-payment [provider wallet_id snapshot_id trace_id viewtoken amount currency]
  (log/debug "==========verify payment=============")
  (log/debug provider)
  (log/debug wallet_id)
  (log/debug snapshot_id)
  (log/debug trace_id)
  (log/debug viewtoken)
  (log/debug amount) 
  (log/debug currency)

  (let [asset-id (try
    ((keyword currency) (eval (symbol (str "prsc.assetmapping/" provider))))
    (catch Exception e (log/error (str "verify-payment " e))))]
    (log/debug (str "asset-id:" asset-id))
    (cond 
        (= provider "mixin.one") (verifyMixinTx snapshot_id viewtoken trace_id)
      :else (make-error nil UNSUPPORTED_PAYMENT_SERVICE_ERROR {:provider provider})
    )
  )
)

(defn verify-contract-bill-receipt [ receipt_tx bill_tx contract_result]
   (let [view_token (:payment_view_token (:data receipt_tx))
      snapshot_id (:payment_snapshot_id (:data receipt_tx))
      receipt_amount (:amount (:meta receipt_tx))
      contract_id (:contract_id (:meta receipt_tx))
      receipt_beneficiary_address (:beneficiary_address (:meta receipt_tx))
      receipt_payer_address (:payer_address (:meta receipt_tx))
      receipt_beneficiary_wallet_id (:beneficiary_wallet_id (:meta receipt_tx))
    ]

    (log/debug (str "view token:" view_token))
    (log/debug (str "snapshot_id:" snapshot_id))
    (log/debug (str "bill_tx" bill_tx))
    ;0 verify address and wallet
    (if (true? (and (= (:beneficiary_address (:meta bill_tx)) receipt_beneficiary_address) (= (:beneficiary_wallet_id (:meta bill_tx)) receipt_beneficiary_wallet_id))) ;0 VERIFY address/wallet of the receipt object match with bill object
      (if (true? (and (= (:address contract_result) receipt_beneficiary_address) (= (:price contract_result) receipt_amount))) ;1 verify address/amount of contract query result match with receipt
        (verify-payment (:payment_provider (:data bill_tx)) (:beneficiary_wallet_id (:meta bill_tx)) snapshot_id (:payment_trace_id (:data bill_tx)) view_token receipt_amount  (:currency contract_result))
        (make-error nil RECEIPT_CONTRACT_PAYMENT_NOT_MATCH_ERROR {:contract_address (:address contract_result) :receipt_beneficiary_address receipt_beneficiary_address :contract_price (:price contract_result) :receipt_amount receipt_amount})
      )
      (make-error nil RECEIPT_BENEFICIARY_ADDRESS_WALLET_NOT_MATCH_ERROR {:beneficiary_address (:beneficiary_address (:meta bill_tx)) :receipt_beneficiary_address receipt_beneficiary_address :beneficiary_wallet_id (:beneficiary_wallet_id (:meta bill_tx)) :receipt_beneficiary_wallet_id receipt_beneficiary_wallet_id })
    )
   )
  ;0: bill == receipt 
  ;0.1: receipt == contract
  ;1: mixin trace_id  == bill_trace_id
  ;2: mixin == bill == receipt
)

(defn parse-PRSData [data]
  (let [
        bill_id (:bill_id (:data data))
        references (:references (:meta data))
        args (filter (fn [[k v]] (some #(= (name k) %) enabled-input-fields)) (reduce merge (map (fn [x] {(keyword (fieldmapping (clojure.string/replace (name x) #"contract_" ""))) (x (:meta data))}) (filter (fn [x] (clojure.string/starts-with? (name x) "contract_")) (keys (:meta data))))))
        ]
    (if (or (nil? bill_id) (nil? references))
      (make-error nil PRS_DATA_ERROR)
      (let [bill_block (block-prsid-mapping bill_id references)]
        (if (or (nil? (:block_num bill_block)) (nil? (:trx_id bill_block)))
          (make-error nil BILL_DATA_ERROR {:bill_id bill_id})
          (let [tx (prsc.eos/get-eos-tx (:block_num bill_block) (:trx_id bill_block))]
              (if (nil? (:err tx))
                (let [billtx (tx-to-bill tx)]
                   (let [contract_id (:contract_id (:meta billtx))]
                    (log/debug (str "contract_id " contract_id))
                    (let [contract_block (block-prsid-mapping contract_id references)]
                      (log/debug (str "contract_block " contract_block))
                      (if (or (nil? (:block_num contract_block)) (nil? (:trx_id contract_block)))
                        (make-error nil CONTRACT_DATA_ERROR {:contract_id contract_id})
                        (let [contract_tx (prsc.eos/get-eos-tx (:block_num contract_block) (:trx_id contract_block))]
                          (let [contract_data (prsc.eos/verifytrx contract_tx)]
                            (if (nil? contract_data)
                              (make-error nil PRS_NOCONTRACT_ERROR {:block_num (:block_num contract_block) :trx_id (:trx_id contract_block)})
	                          (try
                                (let [queryresult (prsc.dsl/run-PRSC (fetch-PRSC contract_data) args prsc.dsl/get-license)]
                                  (let [queryresult (parseQueryResult (:name (meta #'prsc.dsl/get-license)) queryresult)]
                                      (verify-contract-bill-receipt data billtx queryresult)))  ;TOFIX why throw 9006 error?
                                (catch Exception e (log/error (str "parse-PRSData" (.getMessage e))) PRS_RUNCONTRACT_ERROR) ;TOFIX: (:err :reason) foramt
                              )
                            )
                          )
                        )
                      )
                    )
                   )
	      	  )
                tx ;return error
              )
          )
        )
      )
    )))

(defn verify-onchain [blocknum txid]
  (log/debug (str "verify-onchain" blocknum " " txid))
  (let [tx (prsc.eos/get-eos-tx blocknum txid)]
    (if (prserr? tx)
      tx
      (let [data (prsc.eos/verifytrx tx)]
        (if (nil? data)
          (make-error nil PRS_DATA_IS_NULL )
          (let [prsdata (verify-PRSdata data)]
            (if (nil? prsdata)
              (make-error nil PRS_DATA_ERROR)
              (parse-PRSData prsdata))
            )))
    )))


