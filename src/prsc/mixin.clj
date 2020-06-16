(ns prsc.mixin
  "mixin functions"
  (:require [config.core :refer [env]]
            [cheshire.core :refer :all]
            [clojure.tools.logging :as log]
            [org.httpkit.client :as http]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :as keys]
            [buddy.core.hash :as hash]
            [buddy.core.codecs :refer :all]
            [prsc.error :refer :all]
            ))

(defn jwtToken [method uri body opts]
  (let [transfer_sig_str (str method uri body)]
    (let [transfer_sig_sha256 (-> (hash/sha256 transfer_sig_str) (bytes->hex)) 
		  seconds (quot (.getTime (new java.util.Date)) 1000)
          timeout (or (:time-out opts) 3600)
          ]
      (let [seconds_exp (+ seconds timeout)]
        (let [privatekey (keys/private-key (:privatekey opts))]
	      (jwt/sign {:uid (:client_id opts) :sid (:session_id opts) :iat seconds :exp seconds_exp :jti (java.util.UUID/randomUUID) :sig transfer_sig_sha256} privatekey {:alg :rs512})
        )
      )
  ))
)

(defn getMixinTx [uri viewtoken]
  (let [resp (http/get (str (or (:mixinapiroot env) "https://api.mixin.one")  uri) {:timeout 1000 
									  :headers {"Authorization" (str "Bearer " viewtoken)
											 "Content-Type" "application/json"
											}})]
                (if (= 200 (:status @resp))
                  {:status (:status @resp) :data (:data (parse-string (:body @resp) true))}
                  {:status (:status @resp) :data {}}
                )
            )
)

(defn verifyMixinTx [snapshot_id viewtoken trace_id]
  (let [uri (str "/network/snapshots/" snapshot_id)]
      (let [res (getMixinTx uri viewtoken)]
        (log/debug (str "verifyMixinTx : " uri trace_id))
        (log/debug viewtoken)
        (if (not= 200 (:status res))
         (make-error nil MIXIN_SERVICE_ERROR {:status (:status res) :uri uri})
          (if (= (:trace_id (:data res)) trace_id)
            {:result true}
            (make-error false TRACE_ID_NOT_MATCH_ERROR {:l_name "PRS" :l_trace_id trace_id :r_name "MIXIN" :r_trace_id  (:trace_id (:data res)) :r_chain_id  snapshot_id})
          )
        )
      )
  )
)
