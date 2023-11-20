(ns keepa.qr
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell])
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


(defn text-from-qr-code-image-file [file-name]
  (subs (:out (shell/sh "zbarimg" file-name))
        8))

(comment
  (text-from-qr-code-image (text-to-qr-code-image "haa"))

  (text-from-qr-code-image-file "/Users/jukka/Downloads/IMG_8043.HEIC.jpg"))
