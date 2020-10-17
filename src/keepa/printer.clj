(ns keepa.printer
  (:import [java.awt.print PrinterJob Printable]
           [java.awt Color]))

(defn print-image [buffered-image]
  (let [job (PrinterJob/getPrinterJob)]
    (.setPrintable job
                   (reify Printable
                     (print [this graphics page-format page-number]
                       (if (< 0 page-number)
                         Printable/NO_SUCH_PAGE
                         (do
                           (.drawImage graphics
                                       buffered-image
                                       10
                                       10
                                       80
                                       80
                                       ;; (int (.getImageableX page-format))
                                       ;; (int (.getImageableY page-format))
                                       ;; (int (.getImageableWidth page-format))
                                       ;; (int (.getImageableHeight page-format))
                                       (Color/WHITE)
                                       nil)
                           #_(try

                             (catch Exception e
                               (prn e) ;; TODO: remove-me
                               )
                             )
                           #_(.translate graphics
                                         (.getImageableX page-format)
                                         (.getImageableY page-format))

                           #_(.drawString graphics "Hello world!" 100 100)
                           Printable/PAGE_EXISTS)))))
    (if (.printDialog job)
      (.print job))))

(comment
  )
