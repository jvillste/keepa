(ns keepa.core
  (:require [clojure.java.io :as io]
            [keepa.shell :as shell]
            [crypto.password.scrypt :as scrypt]
            [keepa.cryptography :as cryptography]
            [keepa.editor :as editor]
            [keepa.time :as time]
            [keepa.qr :as qr]
            [keepa.image :as image]
            [keepa.web-camera :as web-camera]
            [keepa.printer :as printer])
  (:import [java.awt.image BufferedImage]
           java.awt.Color))

(defn path [& parts]
  (.getPath (apply io/file parts)))

(defn make-directories [path]
  (.mkdirs (io/file path)))

(defn file-name [file-path]
  (.getName (io/file file-path)))


(comment
  (def secret-key (cryptography/encode (cryptography/generate-secret-key "keep2")))
  (editor/show-image (qr/text-to-qr-code-image secret-key))

  (editor/show-image (let [code-image (qr/text-to-qr-code-image "foo")
                           image (BufferedImage. 200 200 BufferedImage/TYPE_INT_ARGB)
                           graphics (.getGraphics image)]
                       (.drawImage graphics
                                   code-image
                                   10
                                   10
                                   80
                                   80
                                   (Color/WHITE)
                                   nil)
                       (.dispose graphics)
                       image))

  (printer/print-image (qr/text-to-qr-code-image "foo"))
  (editor/show-image (web-camera/capture-image-with-ffmpeg))
  (editor/show-image (web-camera/capture-image))
  (image/buffered-image-to-png-file (qr/text-to-qr-code-image secret-key)
                                    "temp/code.png")
  (qr/text-from-qr-code-image (image/image-bytes-to-buffered-image (web-camera/capture-image-with-ffmpeg)))

  (save-file-with-password "temp/test.jpg.password"
                           (capture-image-with-ffmpeg)
                           "foobar")
  (editor/show-image (cryptography/decrypt (io/file "temp/test.jpg.password")
                                           "foobar"))
  )


(defn make-directories [path]
  (.mkdirs (io/file path)))

(defn make-keep [name path]
  (let [secret-key-file (io/file path (str name ".secret"))]
    (cryptography/spit-secret-key name secret-key-file)
    (cryptography/spit-public-key secret-key-file
                                  (io/file path (str name ".public")))))

(defn make-paper-keep [path name storage-details]
  (let [secret-key (->> (cryptography/generate-secret-key name)
                        (cryptography/encode))]
    (image/buffered-image-to-png-file (qr/text-to-qr-code-image (pr-str {:secret-key secret-key
                                                                         :storage storage-details}))
                                      (io/file path (str name ".secret.png")))
    (->> secret-key
         (cryptography/public-key)
         (cryptography/encode)
         (spit (io/file path (str name ".public"))))))

(defn make-master-keep [path]
  (make-directories path)
  (cryptography/spit-secret-key "master"
                                (io/file path "master.secret")))

(defn create-password-hash-file [file-name]
  (spit file-name (scrypt/encrypt (editor/ask-password))))

(defn ask-and-check-password [password-hash-file-name]
  ;; TODO:  do the checking by trying to open the previous version of the file with this password
  (let [password (editor/ask-password)]
    (if (scrypt/check password
                      (slurp password-hash-file-name))
      password
      (do (println "Invalid password")
          nil))))

(defn load-master-file [file-name secret-key password]
  (if (.exists (io/file file-name))
    (-> (slurp file-name)
        (cryptography/decrypt password)
        (cryptography/decrypt secret-key))
    ""))

(defn save-master-file [contents file-name secret-key password]
  (spit file-name
        (-> contents
            (cryptography/encrypt secret-key)
            (cryptography/encrypt password))))

(defn key-combination-file-name [master-file-path public-keys]
  (str master-file-path "_" (apply str (interpose "-" (map cryptography/user-id public-keys)))))

(defn encrypt-with-key-combination [contents public-keys]
  (reduce (fn [data public-key]
            (cryptography/encrypt data
                                  public-key))
          contents
          public-keys))

(defn load-key [path]
  (cryptography/decode (slurp path)))

(defn save-file-encrypted-with-key-combination [contents master-file-path public-keys]
  (spit (key-combination-file-name master-file-path public-keys)
        (encrypt-with-key-combination contents
                                      public-keys)))

(defn decrypt-with-key-combination [data private-keys]
  (reduce (fn [data private-key]
            (cryptography/decrypt data
                                  private-key))
          data
          private-keys))

(defn decrypt-file-with-key-combination [file-path private-key-paths]
  (decrypt-with-key-combination (slurp file-path)
                                (map load-key private-key-paths)))


(defn edit-file-with-key-and-password [file-name secret-key password public-keys key-combination-size]
  (let [new-text-or-cancel (editor/edit (load-master-file file-name secret-key password))]
    (when (not= :cancel new-text-or-cancel)
      (save-master-file new-text-or-cancel
                        file-name
                        secret-key
                        password))))

(defn edit-file [file-name secret-key-file-name password-hash-file public-keys key-combination-size]
  (when-let [password (ask-and-check-password password-hash-file)]
    (edit-file-with-key-and-password file-name
                                     (cryptography/decode (slurp secret-key-file-name))
                                     password
                                     public-keys
                                     key-combination-size)))

(defn load-file-with-password [file-name password]
  (if (.exists (io/file file-name))
    (-> (slurp file-name)
        (cryptography/decrypt password))
    ""))

(defn save-file-with-password [file-name contents password]
  (spit file-name
        (cryptography/encrypt contents password)))

(defn edit-file-with-password [file-name]
  (when-let [password (editor/ask-password)]
    (save-file-with-password file-name
                             (editor/edit (load-file-with-password file-name password))
                             password)))

(defn copy-file-and-encrypt-with-password [source-file-path target-file-path password-hash-file-path]
  (when-let [password (ask-and-check-password password-hash-file-path)]
    (save-file-with-password target-file-path
                             (slurp source-file-path)
                             password)))

(defn copy-file-and-encrypt-with-key-combination [source-file-path target-file-path public-key-file-paths]
  (save-file-encrypted-with-key-combination (slurp source-file-path)
                                            target-file-path
                                            (map load-key public-key-file-paths)))

(defn store-file-path [file-path store-path]
  (path store-path (str (file-name file-path)  "-" (time/local-time-stamp-string))))

(defn copy-file-and-encrypt-with-password-and-key-combination [source-file-path target-file-path password-hash-file-path public-key-file-paths]
  (when-let [password (ask-and-check-password password-hash-file-path)]
    (save-file-with-password target-file-path
                             (slurp source-file-path)
                             password)
    (copy-file-and-encrypt-with-key-combination source-file-path
                                                target-file-path
                                                public-key-file-paths)))

(defn backup-file-and-encrypt-with-password-and-key-combination [source-file-path store-path password-hash-file-path public-key-file-paths]
  (copy-file-and-encrypt-with-password-and-key-combination source-file-path
                                                           (store-file-path source-file-path
                                                                            store-path)
                                                           password-hash-file-path
                                                           public-key-file-paths))


(defn spit-file-and-encrypt-with-password-and-key-combination [contents file-name store-path password-hash-file-path public-key-file-paths]
  (when-let [password (ask-and-check-password password-hash-file-path)]
    (let [store-file-path (store-file-path file-name store-path)]
      (save-file-with-password store-file-path
                               contents
                               password)
      (save-file-encrypted-with-key-combination contents
                                                store-file-path
                                                (map load-key public-key-file-paths)))))

(defn edit-file-with-password-and-keys [file-path pasword-hash-file-path public-key-file-names store-path]
  (when-let [password (ask-and-check-password pasword-hash-file-path)]
    (let [old-contents (load-file-with-password file-path password)
          new-contents (editor/edit old-contents)
          store-file-path (store-file-path file-path store-path)]
      (if (not= old-contents new-contents)
        (do
          (save-file-with-password file-path
                                   new-contents
                                   password)
          (save-file-with-password store-file-path
                                   new-contents
                                   password)
          (save-file-encrypted-with-key-combination new-contents
                                                    store-file-path
                                                    (map load-key public-key-file-names)))
        (println "No changes made.")))))



(defn sync-remote-directory [local-directory username host remote-directory]
  (shell/run-command "rsync" "-a" (str local-directory "/")  (str username "@" host ":" remote-directory)))

(comment
  (make-directories "temp/test")
  (make-keep "master" "temp/test")
  (make-paper-keep "keep1" "temp/test"
                   {:url "http://example.com"
                    :username "foo"
                    :password "bar"})
  
  

  (create-password-hash-file "temp/hash")

  (spit "foobar" "temp/test.txt")

  (copy-file-and-encrypt-with-password "temp/test.txt"
                                       "temp/test.txt.encrypted"
                                       "temp/hash")

  (make-keep "test-keep" "temp" "temp")

  (copy-file-and-encrypt-with-key-combination "temp/test.txt"
                                              "temp/test.txt.encrypted"
                                              ["temp/test-keep.public"])

  (backup-file-and-encrypt-with-password-and-key-combination "temp/test.txt"
                                                             "temp/test.txt.encrypted"
                                                             "temp/hash"
                                                             ["temp/test-keep.public"])

  (decrypt-file-with-key-combination "temp/test.txt.encrypted_test-keep"
                                     ["temp/test-keep.secret"])


  (spit-file-and-encrypt-with-password-and-key-combination "this is secret"
                                                           "very-secret"
                                                           "temp/store"
                                                           "temp/hash"
                                                           ["temp/key-1.public"])

  (-> (slurp "temp/store/very-secret-2018-11-22-19-56-36")
      (cryptography/decrypt "foo"))

  (-> (slurp "temp/store/very-secret-2018-11-22-19-56-36_key-1")
      (cryptography/decrypt (load-key "temp/key-1.secret")))

  (edit-file-with-password-and-keys "temp/secret2"
                                    "temp/hash"
                                    ["temp/key-1.public"]
                                    "temp/store")

  (save-file-encrypted-with-key-combination "foo"
                                            "temp/laptop/foo"
                                            (map load-key ["temp/laptop/1.public"
                                                           "temp/laptop/2.public"]))

  (decrypt-with-key-combination (slurp "temp/laptop/foo-1-2")
                                (map load-key ["temp/2/2.secret"
                                               "temp/1/1.secret"]))
  )
