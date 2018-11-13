(ns prsc.core
  (:gen-class)
  (:require [config.core :refer [env]]
            [clojure.data.json :as json]
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

(defrecord LicenseTerm [type currency price termtext])

(defrecord ContractText [type text])

(defrecord Receiver [address])

(defrecord license-type [name])

(defrecord Linked-license [address type currency price])

(defrecord Linked-result [result])

(defrecord Linked-Contract-List [rid args])

(defn formatdesc [& thedesc]
  [:desc (clojure.string/trim (apply str thedesc))])
(defn formatname [& thename]
  [:name (clojure.string/trim (apply str thename))])

(def prsctransform-options
  {:vernum read-string
   :version (fn [ver]
              {:name "version"
               :lhs ()
               :rhs `(print-ver ~ver)})
   :name (fn [& thename]
           {:name "name"
            :lhs ()
            :rhs ()})
   :desc (fn [& thedesc]
           {:name "desc"
            :lhs ()
            :rhs ()})
   :contract (fn [thetype text]
               {:name (str "contract_" thetype)
                :lhs [{:type license-type
                       :constraints [(list = (symbol "name") thetype)]}]
                :rhs `(insert! (->ContractText ~thetype ~text))})
   :receiver (fn [address]
               {:name "receiver"
                :lhs ()
                :rhs `(insert! (->Receiver ~address))})
   :price (comp str read-string)
   :currency read-string
   :use (fn [rid args]
          {:name rid
           :lhs []
           :rhs `(insert! (->Linked-Contract-List ~rid ~args))})

   :license (fn [thetype currency price termtext]
              {:name thetype
               :lhs [{:type license-type
                      :constraints [(list = (symbol "name") thetype)]}]
               :rhs `(insert! (->LicenseTerm ~thetype ~currency ~price ~(clojure.string/trim termtext)))})})

(def prscparsertransform-options
  {:price (comp str read-string)
   :currency read-string
   :license (fn [thetype currency price termtext] [:license {"type" thetype, "currency" currency, "price" price, "termtext" (clojure.string/trim termtext)}])
   :contract (fn [thetype text] [:contract {"name" thetype, "text" text}])
   :name formatname
   :desc formatdesc})
 ;args nl
(def prscparser
  (insta/parser
   "<statement> = [version | assign | receiver | license | contract | pay | name | desc | use]+
    version = <'PRSC Ver'> space vernum;
    vernum = float;
    assign = <'let'> varname <'='> [ address | string | utf8str ] nl
    receiver = <'Receiver'> address
    name = <'Name'> space utf8stre
    desc = <'Desc'> space utf8stre
    license  = <'License'> licensetype currency <':'> price  <'Terms'> <':'> [ address | string | utf8stre ]
    contract = <'Contract'> licensetype [ address | string | utf8stre ]
    pay = <'Pay'> msghash licensetype currency <':'> price
		use = <'Use'> rid args
    price = float;
    <currency> = 'PRS' | 'CNB';
    <varname> = string;
		<args> = utf8str;
    <string> = #'[A-Za-z0-9_-]+';
    <address> = #'[A-Za-z0-9]{40}';
    <msghash> = #'[A-Za-z0-9]{64}';
    <rid> = #'[A-Za-z0-9]{64}';
    <licensetype> = string;
    <space> = <#'[ ]+'>;
    <utf8str> = #'([^\r\n\"\\\\]|\\s\\\\.,)+';
    <utf8stre>= #'([^\r\n\"]|\\s\\\\.,)+';
    <float> = #'[0-9]+(\\.[0-9]+)?';
    <digit> = #'[0-9]+';
    <nl> =  #'[\r\n]+';
  "
   :auto-whitespace :standard))

(defn print-ver [input]
  (println (str "version: " input)))

(defn prscparse [input]
  (->> (prscparser input) (insta/transform prsctransform-options)))

(defquery get-license
  "Returns the available license"
  []
  [?receiver <- Receiver]
  [?license <- LicenseTerm]
  )

(defquery get-linked-license
  "Returns the linked license"
  []
  [?linkedlicenses <- Linked-license])

(defquery get-linked-result
  "Returns the linked result"
  []
  [?linkedresult <- Linked-result])

(defquery get-license-type
  "Returns the current license type"
  []
  [?licensetype <- license-type])

(defquery get-linked-Contracts
  "Returns the linked license type"
  []
  [?linkedContracts <- Linked-Contract-List])

(s/defn ^:always-validate load-user-rules :- [clara.rules.schema/Production]
  [business-rules :- s/Str]

  (let [parse-tree (prscparser business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info (print-str parse-tree) {:failure parse-tree})))

    (insta/transform prsctransform-options parse-tree)))

(defn fetchContractCodeWithrID [rid]
  (let [resp (http/get (str (:apiroot env) "/blocks/txes?rIds=" rid))]
    (let [block (parse-string (:body @resp) true)]
      (let [contract (parse-string (:data (first (:txes (:data block)))) true)]
        (:code contract)))))

(defn -main [& args]
  (update-global :level 0)
  (if (= true ((complement nil?) (first *command-line-args*)))
    (let [rid (clojure.string/trim (first *command-line-args*))]
      (let [contractcode (cond (= 64 (count rid))
                               (fetchContractCodeWithrID rid)
                               :else (slurp rid))]
        (try
          (let [session (-> (mk-session 'prsc.core (load-user-rules contractcode))
                            (insert
                             (->license-type "Commercial")
                       ;(->Linked-license "add01" "p01" "PRS" "1.1111")
                       ;(->Linked-license "add02" "p02" "PRS" "2.1111")
)
                            (fire-rules))]
            (let [result (query session get-license)]
              (clojure.pprint/pprint result))
            (let [result (apply query [session get-linked-Contracts])]
              (clojure.pprint/pprint result)))

          (catch Exception e
            (clojure.pprint/pprint "===error")
            (clojure.pprint/pprint e))
          (finally
            (printf "/license level: %s%n" (get-global :level))))

      (let [parse-tree (prscparser contractcode)]
        (clojure.pprint/pprint (insta/transform prscparsertransform-options parse-tree)))
))))




(defn format-for-json [k]
  (apply hash-map (assoc k 0 (keyword (clojure.string/replace (first k) #":\?" "")))))

(def app
  (api
   {:api {:invalid-routes-fn nil}}

   (GET "/license/:address" []
     :path-params [address :- String]
     :query-params [licensetype :- String]
     (try
       (let [contractcode (fetchContractCodeWithrID address)]
         (let [session (-> (mk-session 'prsc.core (load-user-rules contractcode))
                           (insert
                            (->license-type licensetype))
                           (fire-rules))]
           (let [result (query session get-license)]
             (let [result-linkedcontracts (query session get-linked-Contracts)]
               (let [newmap (doall
                             (map (fn [k]
                                    (format-for-json k)) (first result)))]
                 (let [newmap1 (doall
                                (map (fn [k]
                                       (format-for-json k)) (first result-linkedcontracts)))]
                   (ok (concat newmap newmap1))))))))

       (catch Exception e
         (ok (->> (ex-data e))))
       (finally
         (println "/license response"))))

   (GET "/parser" []
     :query-params [address :- String]
     (try
       (let [contractcode (fetchContractCodeWithrID address)]
         (let [parse-tree (prscparser contractcode)]
           (when (insta/failure? parse-tree)
             (throw (ex-info (print-str parse-tree) {:failure parse-tree})))
           (ok (->> parse-tree (insta/transform prscparsertransform-options)))))

       (catch Exception e
         (ok (->> (ex-data e))))
       (finally
         (println "/parser response"))))

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

         (let [session (-> (mk-session 'prsc.core (load-user-rules code))
                           (insert
                            (->license-type args))
                           (fire-rules))]

           (let [result (apply query [session (str (symbol "prsc.core" call))])]
             (let [newmap (doall
                           (map (fn [k]
                                  (format-for-json k)) (first result)))]

               (ok newmap)))))
       (catch Exception e
         (ok (->> (ex-data e))))
       (finally
         (println "/repl response"))))

   (undocumented
    (route/resources "/")
    (route/not-found

     "404 Not Found"))))
