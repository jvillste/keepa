(ns keepa.editor
  (:require [clojure.core.async :as async])
  (:import [javax.swing JFrame JTextArea JButton JPanel JPasswordField]
           [java.awt BorderLayout FlowLayout]
           [java.awt.event ActionListener KeyAdapter KeyEvent]))

(defn edit [text on-save]
  (let [j-frame (JFrame. "Editor")
        root-j-panel (JPanel. (BorderLayout.))
        button-j-panel (JPanel. (FlowLayout.))
        text-area (JTextArea. text)
        save-button (JButton. "Save")
        cancel-button (JButton. "Cancel")]
    (.addActionListener save-button
                        (reify ActionListener
                          (actionPerformed [this event]
                            (on-save (.getText text-area))
                            (.dispose j-frame))))

    (.addActionListener cancel-button
                        (reify ActionListener
                          (actionPerformed [this event]
                            (.dispose j-frame))))
    (.add root-j-panel text-area BorderLayout/CENTER)
    (.add root-j-panel button-j-panel BorderLayout/PAGE_END)
    (.add button-j-panel save-button)
    (.add button-j-panel cancel-button)
    (.setContentPane j-frame root-j-panel)
    (.pack j-frame)
    (.setVisible j-frame true)))

(defn ask-password []
  (let [j-frame (JFrame. "Editor")
        password-field (JPasswordField.)
        channel (async/chan)]

    (.addKeyListener password-field (proxy [KeyAdapter] []
                                      (keyPressed [event]
                                        (if (= (.getKeyCode event)
                                               (KeyEvent/VK_ENTER))
                                          (do (async/>!! channel (.getText password-field))
                                              (.dispose j-frame))))))

    (.add j-frame password-field)
    (.pack j-frame)
    (.setVisible j-frame true)
    (async/<!! channel)))

(comment
  (edit "haa"
        (fn [saved] (println saved)))

  (ask-password (fn [password] (println password)))
  )
