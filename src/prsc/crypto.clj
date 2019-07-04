(ns prsc.crypto
  "crypto functions"
  (:require [config.core :refer [env]]
            [secp256k1.core :as secp256k1]))

(defn signature [signtext]
  (-> (str (:privkey env)) (secp256k1/private-key :hex) (secp256k1/sign signtext)))

