(ns prsc.eos
  "eos chain functions"
  (:require [config.core :refer [env]]
            [cheshire.core :refer :all]
            [org.httpkit.client :as http]
            [prsc.error :refer :all]
            [prsc.utils :refer :all]
            )
    (:import [java.net URI]
             [javax.net.ssl SNIHostName SNIServerName SSLEngine SSLParameters])
  )

(defn sni-configure
  [^SSLEngine ssl-engine ^URI uri]
  (let [^SSLParameters ssl-params (.getSSLParameters ssl-engine)]
    (.setServerNames ssl-params [(SNIHostName. (.getHost uri))])
    (.setUseClientMode ssl-engine true)
    (.setSSLParameters ssl-engine ssl-params)))


(def sni-client (http/make-client {:timeout 1000 :headers { "Content-Type" "application/json" } :ssl-configurer sni-configure}))

(defn trxbyid [v trx_id]
  (filter (fn [x] (if (= (clojure.string/lower-case trx_id) (clojure.string/lower-case (:id (:trx x)))) x)) v)
)

(defn verifytrx [trx]
  ;verify sign
  (let [trx (:trx (prsc.utils/safenth (:tx trx) 0))]
    (if (nil? trx)
      nil
      (:data (prsc.utils/safenth (:actions (:transaction trx)) 0))
    )
  )
)

(defn get-eos-tx [block_num trx_id]
  (let [resp (http/get (str (or (:chainapi env) "https://prs-bp4.press.one/api/chain/blocks/") block_num) {:client sni-client})]
                (if (= 200 (:status @resp))
                  {:status (:status @resp) :tx (trxbyid (:transactions (:data (parse-string (:body @resp) true))) trx_id) }
                  (make-error nil PRS_CHAIN_SERVICE_ERROR {:status (:status @resp) :block_num block_num})
                )
            )
)

(defn fetchContractCodeWithrID [contract_id]
  (let [resp (http/get (str (:apiroot env) "/blocks/" contract_id))]
    (let [block (parse-string (:body @resp) true)]
      (if (nil? (:data (first block)))
        (make-error nil PRS_NOCONTRACT_ERROR {:rID contract_id})
        (:code (parse-string (:data (first block)) true))
      )
      )))
