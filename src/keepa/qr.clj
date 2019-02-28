(ns keepa.qr
  (:require [clojure.java.io :as io])
  (:import [com.google.zxing BarcodeFormat BinaryBitmap MultiFormatReader]
           [com.google.zxing.client.j2se BufferedImageLuminanceSource MatrixToImageWriter]
           com.google.zxing.common.HybridBinarizer
           com.google.zxing.qrcode.QRCodeWriter
           javax.imageio.ImageIO))

(defn text-to-qr-code-image [text]
  (MatrixToImageWriter/toBufferedImage (.encode (QRCodeWriter.)
                                                text
                                                BarcodeFormat/QR_CODE
                                                1000
                                                1000)))


(defn text-from-qr-code-image [buffered-image]
  (.getText (.decode (MultiFormatReader.)
                     (BinaryBitmap. (HybridBinarizer. (BufferedImageLuminanceSource. buffered-image))))))


(comment
  (text-from-qr-code-image (text-to-qr-code-image "haa")))
