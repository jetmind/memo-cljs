(defproject memo-cljs "0.1.0-SNAPSHOT"
  :description "Memo: simple memory game"
  :url "http://github.com/jetmind/memo-cljs"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.3.0"]
                 [com.facebook/react "0.8.0.1"]]

  :plugins [[lein-cljsbuild "1.0.1"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "memo-cljs"
              :source-paths ["src"]
              :compiler {
                :output-to "memo_cljs.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
