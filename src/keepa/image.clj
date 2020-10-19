(ns keepa.image
  (:import javax.imageio.ImageIO
           java.awt.image.BufferedImage
           java.io.ByteArrayInputStream)
  (:require [clojure.java.io :as io]))

(defn file-to-buffered-image [file-name]
  (ImageIO/read (io/file file-name)))

(defn image-bytes-to-buffered-image [data]
  (ImageIO/read (ByteArrayInputStream. data)))

(defn buffered-image-to-png-file [buffered-image file-name]
  (ImageIO/write buffered-image
                 "png"
                 (io/file file-name)))
