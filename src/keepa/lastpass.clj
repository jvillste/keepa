(ns keepa.lastpass
  (:require [keepa.clipboard :as clipboard]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import [org.apache.commons.csv CSVFormat CSVParser])
  (:use [clojure.test]))

(defn parse-csv [reader]
  (map (fn [row]
         (into [] (iterator-seq (.iterator row))))
       (.getRecords (CSVParser. reader
                                CSVFormat/EXCEL))))

(defn string-reader [string]
  (java.io.StringReader. string))

(defn parse-lastpass-export [export-string]
  (let [rows (parse-csv (string-reader export-string))
        keys (map keyword (first rows))]
    (map (fn [row]
           (zipmap keys
                   row))
         (rest rows))))

(defn filter-by-group [allowed-groups rows]
  (let [allowed-groups-set (set allowed-groups)]
    (filter (fn [row]
              (allowed-groups-set (:grouping row)))
            rows)))

(defn read-lastapass-rows-from-clipboard [groups]
  (->> (clipboard/slurp-clipboard)
       (parse-lastpass-export)
       (filter-by-group groups)))

(comment
  (def data (clipboard/slurp-clipboard))

  (read-lastapass-rows-from-clipboard #{"omat"})

  (->> data
       (parse-lastpass-export)
       (filter-by-group #{"omat"}))


  )
