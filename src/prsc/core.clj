(ns prsc.core
  (:gen-class)
  (:require [prsc.utils :refer :all]
            [prsc.crypto :refer :all]
            [prsc.mixin :refer :all]
            [prsc.eos :refer :all]
            [prsc.dsl :refer :all]
            [prsc.verify :refer :all]
            [prsc.error :refer :all]
            [config.core :refer [env]]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all :as response]
            [org.httpkit.client :as http]
            [instaparse.core :as insta]
            [clara.rules :refer :all]
            [schema.core :as s]
            [cheshire.core :refer :all]))


(def globalvar (atom {}))

(defn get-global [key]
  (@globalvar key))

(defn update-global [key val]
  (swap! globalvar assoc key val))

(defn prscparse [input]
  (->> (prscparser input) (insta/transform prsctransform-options)))

(defn -main [& args]
  ;;  (update-global :level 0)
  (if (= true ((complement nil?) (first *command-line-args*)))

    ;;(let [arg (clojure.string/trim (first *command-line-args*))]
    ;;    (if (= "createkeys" arg)
    ;;        (println (test-sign "test crypto"))
    ;;        (System/exit 0)
    ;;))
    (let [rid (clojure.string/trim (first *command-line-args*))]
      (let [contractcode (cond (= 64 (count rid))
                               (fetchContractCodeWithrID rid)
                               :else (slurp rid))]
        (try
          (let [session (-> (mk-session 'prsc.dsl (load-user-rules contractcode))
                            (insert
                             (->license-type "Commercial")
                       ;(->Linked-license "add01" "p01" "PRS" "1.1111")
                       ;(->Linked-license "add02" "p02" "PRS" "2.1111")
)
                            (fire-rules))]
            (let [result (query session get-license)]
              (clojure.pprint/pprint result))
            (let [result (apply query [session get-linked-Contracts])]
              (clojure.pprint/pprint result))
            (let [result (apply query [session get-multireceivers])]
              (clojure.pprint/pprint result))
            (let [result (apply query [session get-deposit])]
              (clojure.pprint/pprint result))
            (let [result (apply query [session get-principals])]
              (clojure.pprint/pprint result)))

          (catch Exception e
            (clojure.pprint/pprint "===error")
            (clojure.pprint/pprint e))
          (finally
            (printf "/license level: %s%n" (get-global :level))))

        (let [parse-tree (prscparser contractcode)]
          (clojure.pprint/pprint (insta/transform prscparsertransform-options parse-tree)))))))

(defn format-for-json [k]
  (apply hash-map (assoc k 0 (keyword (clojure.string/replace (first k) #":\?" "")))))

(defn parseArgs [argstr]
  (let [args (clojure.string/split argstr #",")]
    args))

(def app
  (api
   {:api {:invalid-routes-fn nil}}
   (context "/v2" []
     :tags ["v2"]

     (GET "/echo" []
       :query-params [q :- String]

       ;(log/error  "error /echo")
       ;(log/debug  "debug /echo")
       ;(log/info "info /echo")
       (try
         (let [signtext (str "/echo" "$" q "$" q)]
           (let [result {:?result  (Integer/parseInt q)}]
             (update-header (update-header (ok (format-for-json (first result))) "signtext"  (fn [arg] (-> signtext))) "sig"  (fn [arg] (-> (signature (apply str signtext)))))))

         (catch Exception e
           (log/error (str "GET /echo" e))
           (ok (->> (ex-data e))))
         (finally (log/info "/echo response"))
         ))

     (GET "/verify/:block_num/:tx_id" []
       :path-params [block_num :- String tx_id :- String]
       :query-params [session :- String]
       (try
         (let [signtext (str "/verify" "$" block_num "$" tx_id "$" session)]
           (let [result (verify-onchain block_num tx_id)]
             (if (= true (instance? Boolean (:result result)))
                (update-header (update-header (ok (assoc result :session session)) "signtext"  (fn [arg] (-> signtext))) "sig"  (fn [arg] (-> (signature (apply str signtext)))))
                (update-header (update-header (ok (assoc result :session session)) "signtext"  (fn [arg] (-> signtext))) "sig"  (fn [arg] (-> (signature (apply str signtext)))))
             )))

         (catch Exception e
           (log/error (str "GET /verify" e))
           (ok (->> (ex-data e))))
         (finally
           (log/debug "GET /verify done"))))
     (GET "/license/:address" []
       :path-params [address :- String]
       :query-params [licensetype :- String]
       (try
         (let [contractcode (fetchContractCodeWithrID address)]
           (let [session (-> (mk-session 'prsc.dsl (load-user-rules contractcode))
                             (insert
                              (->license-type licensetype))
                             (fire-rules))]
             (let [result (query session get-license)]
               (let [result-linkedcontracts (query session get-linked-Contracts)]
                 (let [newmap (doall
                               (map (fn [k]
                                      (format-for-json k)) (first result)))]
                   (let [signtext (for [{{address :address} :?receiver {type :type currency :currency price :price termtext :termtext} :?license} result]
                                    (str "/license/" address "$" licensetype "$" "address,type,currency,price" address type currency price))]
                     (update-header  (update-header (ok newmap) "signtext"  (fn [arg] (-> signtext))) "sig"  (fn [arg] (-> (signature (apply str signtext)))))))))))

         (catch Exception e
           (log/error (str "GET /license " e))
           (ok (->> (ex-data e))))
         (finally
           (log/debug "GET /license done"))))
     (GET "/test" []
       (ok (str "{'message':'ok'}")))
     (GET "/parser" []
       :query-params [address :- String]
       (try
         (let [contractcode (fetchContractCodeWithrID address)]
           (let [parse-tree (prscparser contractcode)]
             (when (insta/failure? parse-tree)
               (throw (ex-info (print-str parse-tree) {:failure parse-tree})))
             (ok (->> parse-tree (insta/transform prscparsertransform-options)))))

         (catch Exception e
           (log/error (str "GET /parser " e))
           (ok (->> (ex-data e))))
         (finally
           (log/debug "GET /parser done"))))

     (POST "/repl" []
       :body-params [code :- String,
                     call :- String,
                     args :- String]

       (try
         (if (= call "")
           (let [parse-tree (prscparser code)]
             (when (insta/failure? parse-tree)
               (throw (ex-info (print-str parse-tree) {:failure parse-tree})))
             (ok (->> parse-tree (insta/transform prscparsertransform-options))))
           (let [session (-> (mk-session 'prsc.dsl (load-user-rules code))
                             (insert
                              (->license-type args))
                             (fire-rules))]
             (let [result (apply query [session (str (symbol "prsc.dsl" call))])]
               (let [newmap (doall
                             (map (fn [k]
                                    (format-for-json k)) (first result)))]

                 (ok newmap)))))
         (catch Exception e
           (log/error (str "POST /repl " e))
           (ok (->> (ex-data e))))
         (finally
           (log/debug "GET /repl response")))))


   ;(GET "/license/:address" []
   ;  :path-params [address :- String]
   ;  :query-params [licensetype :- String]
   ;  (try
   ;    (let [contractcode (fetchContractCodeWithrID address)]
   ;      (let [session (-> (mk-session 'prsc.dsl (load-user-rules contractcode))
   ;                        (insert
   ;                         (->license-type licensetype))
   ;                        (fire-rules))]
   ;        (let [result (query session get-license)]
   ;          (let [result-linkedcontracts (query session get-linked-Contracts)]
   ;            (let [newmap (doall
   ;                          (map (fn [k]
   ;                                 (format-for-json k)) (first result)))]
   ;              (let [newmap1 (doall
   ;                             (map (fn [k]
   ;                                    (format-for-json k)) (first result-linkedcontracts)))]
   ;                (ok (concat newmap newmap1))))))))

   ;    (catch Exception e
   ;      (log/error (str "GET /license/:address " e))
   ;      (ok (->> (ex-data e))))
   ;    (finally
   ;      (log/debug "GET /license/:address done"))))

   ;(GET "/parser" []
   ;  :query-params [address :- String]
   ;  (try
   ;    (let [contractcode (fetchContractCodeWithrID address)]
   ;      (let [parse-tree (prscparser contractcode)]
   ;        (when (insta/failure? parse-tree)
   ;          (throw (ex-info (print-str parse-tree) {:failure parse-tree})))
   ;        (ok (->> parse-tree (insta/transform prscparsertransform-options)))))

   ;    (catch Exception e
   ;      (log/error (str "GET /parser " e))
   ;      (ok (->> (ex-data e))))
   ;    (finally
   ;      (log/debug "GET /parser done"))))


   (undocumented
    (route/resources "/")
    (route/not-found
     "404 Not Found"))))
