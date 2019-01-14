(ns keepa.editor
  (:require [clojure.core.async :as async])
  (:import [javax.swing JFrame JTextArea JButton JPanel JPasswordField JScrollPane JLabel ImageIcon]
           [java.awt BorderLayout FlowLayout]
           [java.awt.event ActionListener KeyAdapter KeyEvent]))

(defn edit [text]
  (let [j-frame (JFrame. "Editor")
        root-j-panel (JPanel. (BorderLayout.))
        button-j-panel (JPanel. (FlowLayout.))
        text-area (JTextArea. text)
        scroll-pane (JScrollPane. text-area)
        save-button (JButton. "Save")
        cancel-button (JButton. "Cancel")
        channel (async/chan)]
    (.addActionListener save-button
                        (reify ActionListener
                          (actionPerformed [this event]
                            (async/>!! channel (.getText text-area))
                            (.dispose j-frame))))

    (.addActionListener cancel-button
                        (reify ActionListener
                          (actionPerformed [this event]
                            (async/>!! channel :cancel)
                            (.dispose j-frame))))
    (.add root-j-panel scroll-pane BorderLayout/CENTER)
    (.add root-j-panel button-j-panel BorderLayout/PAGE_END)
    (.add button-j-panel save-button)
    (.add button-j-panel cancel-button)
    (.setContentPane j-frame root-j-panel)
    (.pack j-frame)
    (.setVisible j-frame true)
    (let [result (async/<!! channel)]
      (if (= :cancel result)
        text
        result))))

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
  (edit "haa")

  (ask-password)
  )


(defn show-image [data]
  (let [j-frame (JFrame. "Editor")
        root-j-panel (JPanel. (BorderLayout.))
        scroll-pane (JScrollPane. (JLabel. (ImageIcon. data)))]
    (.add root-j-panel scroll-pane BorderLayout/CENTER)
    (.setContentPane j-frame root-j-panel)
    (.pack j-frame)
    (.setVisible j-frame true)))
