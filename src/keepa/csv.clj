(ns keepa.csv
  (:require [clojure.string :as string]
            [clojure.test :refer :all]
            [clojure.java.io :as io]))

(defn split-line [string]
  (map string/trim
       (string/split (str string " ")
                     #";")))

(deftest test-split-line
  (testing "empty columns must be empty strings in result"
    (is (= '("1" "" "2" "")
           (split-line "1;;2;")))))

(defn transduce-csv-lines [input-stream encoding transducer reducer]
  (with-open [rdr (io/reader input-stream
                             :encoding encoding)]
    (transduce (comp (map split-line)
                     transducer)
               reducer
               (line-seq rdr))))

"http://sn,,,\"test note with , coma
and line feed\",test note,,0"
