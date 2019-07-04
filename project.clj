(defproject prsc "0.1.1-SNAPSHOT"
  :description "PRS DSL script interpreter"
  :url "https://press.one"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :main prsc.core
  :aot [prsc.core]
  :profiles {:uberjar {:aot :all :resource-paths ["config/prod" "lib/secp256k1-1.0.3.jar"]}
             :dev {:resource-paths ["config/dev" "lib/secp256k1-1.0.3.jar"]}
             :prod {:resource-paths ["config/prod" "lib/secp256k1-1.0.3.jar"]} }
  :ring {:handler prsc.core/app :port 3001}
  :plugins [ [lein-ring "0.12.5"] [lein-cljfmt "0.6.0"]]
  :repositories []
  :managed-dependencies [[org.clojure/core.rrb-vector "0.0.13"]
                       [org.flatland/ordered "1.5.7"]]
  :dependencies [
    [org.clojure/clojure "1.9.0"], 
    [org.clojure/data.json "0.2.6"],
    [metosin/compojure-api "1.1.11"],
    [http-kit "2.4.0-alpha3"]
    [instaparse "1.4.9"], 
    [com.cerner/clara-rules "0.18.0"]
    [org.apache.commons/commons-compress "1.18"]
    [javax.xml.bind/jaxb-api "2.3.0"]
    [org.bouncycastle/bcprov-jdk15on "1.61"]
    [cheshire "5.8.0"]
    [yogthos/config "1.1.1"]
  ]
)
