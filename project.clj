(defproject keepa "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.google.zxing/javase "3.3.3"]
                 [mvxcvi/clj-pgp "0.9.0"]
                 [crypto-password "0.2.0"]
                 [clj-time "0.14.4"]
                 [org.clojure/math.combinatorics "0.1.4"]
                 [org.clojure/core.async "0.4.474"]
                 [org.apache.commons/commons-csv "1.6"]
                 [com.github.sarxos/webcam-capture "0.3.10"]
                 [clj-http "2.0.1"]
                 [hickory "0.7.1"]]
  :main keepa.main)
