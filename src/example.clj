(ns example
  (:require [keepa.core :as core]
            [keepa.cryptography :as cryptography]))

(def master-path "temp/example-master")
(def hash-file-path (core/path master-path "master-hash"))
(def public-key-paths [(core/path master-path "example-keep.public")])
(def store-path (core/path master-path "store"))

(comment
  (core/make-keep "example-keep" "temp/example-keep")

  (core/make-paper-keep "example-paper-keep"
                        "temp/example-paper-keep"
                        {:url "http://example.com"
                         :username "foo"
                         :password "bar"})

  (core/create-password-hash-file hash-file-path)

  (core/spit-file-and-encrypt-with-password-and-key-combination "my secret message"
                                                                "my-secret-message"
                                                                store-path
                                                                hash-file-path
                                                                public-key-paths)

  (core/backup-file-and-encrypt-with-password-and-key-combination "/Users/example-user/.ssh/id_rsa"
                                                                  store-path
                                                                  hash-file-path
                                                                  public-key-paths)

  (core/edit-file-with-password-and-keys (core/path master-path "my-secret-editable-file")
                                         hash-file-path
                                         public-key-paths
                                         store-path)

  (core/sync-remote-directory store-path
                              "example-user"
                              "example.com"
                              "example-keep")

  (cryptography/decrypt (slurp (core/path store-path "my-secret-message-2018-11-22-20-05-34_example-keep") )
                        (core/load-key "/Volumes/example-keep/example-keep.secret"))

  (cryptography/decrypt (slurp (core/path store-path "my-secret-message-2018-11-22-20-05-34_example-keep") )
                        (core/ask-and-check-password hash-file-path))

  )
