(ns prsc.dsl
  "prsc dsl"
  (:require [config.core :refer [env]]
            [cheshire.core :refer :all]
            [instaparse.core :as insta]
            [clara.rules :refer :all]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [prsc.utils :refer :all]
            ))


(defrecord LicenseTerm [type currency price termtext])

(defrecord ContractText [type text])

(defrecord Receiver [address])

(defrecord Deposit [service address currency price total unlock])

(defrecord multi-receiver [address currency p])

(defrecord Principals [address])

(defrecord license-type [name])

(defrecord Linked-license [address type currency price])

(defrecord Linked-result [result])

(defrecord Linked-Contract-List [rid args])

(defrecord Enabledfuncs [name])

(defn create-record [fnname args]
  (cond
    (= "license-type" fnname) (->license-type args)
    :else nil
  )
)

(defquery get-license
  "Returns the available license"
  []
  [?receiver <- Receiver]
  [?license <- LicenseTerm])

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

(defquery get-multireceivers
  "Returns multi receivers"
  []
  [?MultiReceivers <- multi-receiver])

(defquery get-principals
  "Returns multi receivers"
  []
  [?Principal <- Principals])

(defquery get-funcs
  "Returns multi receivers"
  []
  [?Func <- Enabledfuncs])

(defquery get-deposit
  "Returns the despoit info"
  []
  [?Deposit <- Deposit])



(defn formatdesc [& thedesc]
  [:desc (clojure.string/trim (apply str thedesc))])

(defn formatname [& thename]
  [:name (clojure.string/trim (apply str thename))])


(def prscparsertransform-options
  {:price (comp str read-string)
   :currency read-string
   :license (fn [thetype currency price termtext] [:license {"type" thetype, "currency" currency, "price" price, "termtext" (clojure.string/trim termtext)}])
   :contract (fn [thetype text] [:contract {"name" thetype, "text" text}])
   :name formatname
   :desc formatdesc})
 ;args nl


(defn print-ver [input]
  (log/info (str "version: " input))
)

(defn process-deposit [depositservice address currency price total unlock]
  (if (and (= "XIN" depositservice) (uuid? (java.util.UUID/fromString address)))
    (insert! (->Deposit depositservice address currency price total unlock))
    (throw (Exception. "deposit service or address format error."))))

(defn parsemultireceiver [& receiverlist]
;verify: not more than 100%
  (let [sumshares (apply + (map (fn [x]
                                  (read-string (if (= 3 (count x)) (clojure.string/replace (nth x 2 "0") #"%" "") "0"))) (first receiverlist)))]
    (if-not
     (= 0.0 (- 100.00 sumshares))
      (throw (Exception. (str "the sum of receiver shares should equal 100%, current: " sumshares "%")))
      (doall (map (fn [x]
                    (if (= 3 (count x))
                      (insert! (->multi-receiver (nth x 1 "0") "nil" (nth x 2 "0")))
                      (insert! (->multi-receiver (nth x 1 "0") (nth x 2 "0") (nth x 3 "0"))))) (first receiverlist))))))

(defn process-principals [& principallist]
  (doall (map (fn [x]
                (insert! (->Principals x)))  (first principallist))))

(defn process-funcs [& funcs]
  (doall (map (fn [x]
                (log/info (str "process-funcs: " x))
                (insert! (->Enabledfuncs x)))  (first funcs))))


(def prscparser
  (insta/parser
   "<statement> = [version | assign | receiver | license | contract | pay | desc | use | holder | multireceiver | name | deposit | principals | func]+
    version = <'PRSC Ver'> space vernum;
    vernum = float;
    assign = <'let'> varname <'='> [ address | string | utf8str ] nl
    receiver = <'Receiver'> address
    name = <'Name'> space utf8stre
    multireceiver = <'MultiReceiver'> receiver_info+
    holder = <'Holder'> address
    deposit = <'Deposit'> depositservice <':'> string <'Principal'> currency <':'> price <'Total'> <':'> digit <'Unlock'> <':'> digit
    desc = <'Desc'> space utf8stre
    principals = <'Principals'> space address+
    func = <'Func'> space string+
    license  = <'License'> licensetype currency <':'> price  <'Terms'> <':'> [ address | string | utf8stre ]
    contract = <'Contract'> licensetype [ address | string | utf8stre ]
    pay = <'Pay'> msghash licensetype currency <':'> price
    receiver_info = address [(currency <':'> price) | percent]
	use = <'Use'> rid args
    price = float;
    <percent> = #'[0-9]\\d?(?:\\.\\d{1,2})?%';
    <currency> = 'PRS' | 'CNB';
    <depositservice> = 'XIN';
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
   :deposit (fn [depositservice address currency price total unlock]
              {:name "deposit"
               :lhs ()
               :rhs `(process-deposit ~depositservice ~address ~currency ~price ~total ~unlock)})
   :principals (fn [& principallist]
                 {:name "principals"
                  :lhs ()
                  :rhs `(process-principals (apply list '~principallist))})
   :receiver (fn [address]
               {:name "receiver"
                :lhs ()
                :rhs `(insert! (->Receiver ~address))})
   :multireceiver (fn [& receiverlist]
                    {:name "multireceiver"
                     :lhs ()
                     :rhs `(parsemultireceiver (apply list '~receiverlist))})
   :price (comp str read-string)
   :currency read-string
   :depositservice read-string
   :use (fn [rid args]
          {:name rid
           :lhs []
           :rhs `(insert! (->Linked-Contract-List ~rid ~args))})
   :func (fn [& funclist]
           {:name "func"
            :lhs ()
            :rhs `(process-funcs (apply list '~funclist))})
   :license (fn [thetype currency price termtext]
              {:name thetype
               :lhs [{:type license-type
                      :constraints [(list = (symbol "name") thetype)]}]
               :rhs `(insert! (->LicenseTerm ~thetype ~currency ~price ~(clojure.string/trim termtext)))})})

(s/defn ^:always-validate load-user-rules :- [clara.rules.schema/Production]
  [business-rules :- s/Str]

  (let [parse-tree (prscparser business-rules)]

    (when (insta/failure? parse-tree)
      (throw (ex-info ( print-str parse-tree) {:failure parse-tree})))

    (insta/transform prsctransform-options parse-tree)))


(defn run-PRSC [contractcode args resultfn]
	  (let [session (mk-session 'prsc.dsl (load-user-rules  contractcode))]
      (let [inserted-session (apply insert (into [] (concat (list session) (map (fn [input] (create-record (name (input 0)) (input 1))) args))))]
        (let [fired-session (fire-rules inserted-session)]
          (query fired-session resultfn))))
      )
