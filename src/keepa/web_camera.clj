(ns keepa.web-camera
  (:import [com.github.sarxos.webcam Webcam]
           java.awt.Dimension
           com.github.sarxos.webcam.WebcamResolution)
  (:require [clojure.java.shell :as shell]))

(defn capture-image []
  (let [camera (Webcam/getDefault)
        resolution (.getSize WebcamResolution/HD720)]
    (try
      (.close camera)
      (.setCustomViewSizes camera (into-array Dimension [resolution]))
      (.setViewSize camera resolution)
      (.open camera)
      (.getImage camera)
      (finally
        (.close camera)))))

(defn capture-image-with-ffmpeg []
  (let [result (shell/sh "ffmpeg"
                         "-f" "avfoundation"
                         "-video_size" "1280x720"
                         "-framerate" "30"
                         "-i" "0"
                         "-vframes" "1"
                         "-f" "mjpeg"
                         "pipe:1"
                         :out-enc :bytes)]
    (:out result)))
