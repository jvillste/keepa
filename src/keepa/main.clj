(ns keepa.main
  (:require [keepa.printer :as printer]
            [keepa.qr :as qr]))

(defn -main [& command-line-arguments]
  (printer/print-image (qr/text-to-qr-code-image "foo")))
