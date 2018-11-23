(ns keepa.time
  (:require [clojure.string :as string]
            [clj-time.local :as local]
            [clj-time.format :as format]
            [clj-time.core :as clj-time]))

(defn local-time-stamp-string []
  (format/unparse (format/formatter "yyyy-MM-dd-HH-mm-ss" (clj-time/default-time-zone))
                  (local/local-now)))
