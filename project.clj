(defproject prsc "0.1.0-SNAPSHOT"
  :description "PRS light contract interpreter"
  :url "https://press.one"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main prsc.core
  :aot [prsc.core]
  :profiles {:uberjar {:aot :all}
             :dev {:resource-paths ["config/dev"]}
             :prod {:resource-paths ["config/prod"]} }
  :ring {:handler prsc.core/app :port 3001}
  :plugins [[cider/cider-nrepl "0.18.0"] [lein-ring "0.12.0"] [lein-cljfmt "0.6.0"]]
  :dependencies [
    [org.clojure/clojure "1.9.0"], 
    [org.clojure/data.json "0.2.6"],
    [metosin/compojure-api "1.1.11"],
    [http-kit "2.2.0"],
    [instaparse "1.4.9"], 
    [com.cerner/clara-rules "0.18.0"]
    [org.apache.commons/commons-compress "1.18"]
    [cheshire "5.8.0"]
    [yogthos/config "1.1.1"]
  ]
)
