(ns example
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [keepa.core :as core]
   [keepa.cryptography :as cryptography]
   [keepa.shell :as shell]
   [keepa.store :as store]
   [keepa.qr :as qr]))

(def master-path "temp/example-master")
(def hash-file-path (core/path master-path "master-hash"))
(def store-path (core/path master-path "store"))

(defn encryption-keys [password]
  [{:key password
    :name "string"}
   {:key (core/load-key "paper-keep-1.public")
    :name "paper-1"}
   {:key (core/load-key "paper-keep-2.public")
    :name "paper-2"}
   {:key (core/load-key "master-keep.public")
    :name "master"}])

(defn sync-remote-directory [local-directory username host remote-directory]
  (shell/run-command "rsync"  "-e" "ssh -i ~/.ssh/id_ecdsa" "-a" (str local-directory "/")  (str username "@" host ":" remote-directory)))

(defn private-keys []
  {"string" (core/ask-and-check-password hash-file-path)
   "master" (core/load-key (core/path master-path "master-keep/master-keep.secret"))
   ;; "paper-1" (core/load-paper-keep-key-from-image-file "paper-1.jpg")
   ;; "paper-2" (core/load-paper-keep-key-from-image-file "paper-2.jpg")
   })

(defn spit-encrypted [secret-name contents]
  (core/spit-encrypted store-path
                       secret-name
                       (encryption-keys (core/ask-and-check-password hash-file-path))
                       contents))

(defn decrypt [file-name]
  (core/decrypt (private-keys)
                (edn/read-string (slurp file-name))))



(comment

  ;; creating keeps

  (store/set-password "example.com"
                      "keep1"
                      "foo")

  (core/make-keep "keep1" "/Volumes/KEEP1")

  (core/make-keep "master-keep" (core/path master-path "master-keep"))

  (core/make-paper-keep "paper-keep-3"
                        "local/paper-keep-3"
                        [{:url "example.com/public/secret"
                          :username "user"
                          :password "pwd"}])

  (spit (core/path master-path "paper-keep-1.public")
        (cryptography/encode (cryptography/public-key (core/load-paper-keep-key-from-image-file "paper.jpg"))))

  (core/create-password-hash-file hash-file-path)

  (qr/text-from-qr-code-image-file "paper-1.jpeg")




  ;; encryption

  (do (def keepass-export-file "keepass.csv")

      (spit-encrypted "data"
                      (slurp keepass-export-file))

      (io/delete-file keepass-export-file))

  (spit-encrypted "some-file-encrypted" (slurp "some-file"))


  ;; remote storage sync

  (sync-remote-directory store-path
                         "user"
                         "example.com"
                         "/var/www/html/public/")


  ;; decrypt


  (decrypt "encrypted-file")

  )
