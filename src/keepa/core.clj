(ns keepa.core
  (:require [clj-pgp.generate :as generate]
            [clj-pgp.core :as pgp]
            [clj-pgp.keyring :as keyring]
            [keepa.message :as message]
            [crypto.password.scrypt :as scrypt]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clj-time.core :as clj-time]
            [clj-time.format :as format]
            [clojure.math.combinatorics :as combinatorics]
            [keepa.editor :as editor]
            [clojure.test :refer :all])
  (:import [org.bouncycastle.openpgp PGPSecretKey PGPSecretKeyRing]))


(extend-protocol pgp/Encodable

  PGPSecretKey

  (encode [secretkey]
    (.getEncoded secretkey)))

(defn run-command [& args]
  (let [result (apply shell/sh args)]
    (when (or (not= 0 (:exit result))
              (not= "" (:err result)))
      (throw (ex-info (:err result) result)))
    (:out result)))

(defn generate-secret-key [id]
  (.getSecretKey (:secret (generate/generate-keys id ""
                                                  (master-key (keypair (generate/rsa-keypair-generator 3072)
                                                                       :rsa-general))))))

(defn public-key [secret-key]
  (.getPublicKey secret-key))

(defn encode [secret-or-public-key]
  (pgp/encode-ascii secret-or-public-key))

(defn decode [secret-or-public-key-data]
  (let [keyring (first (pgp/decode secret-or-public-key-data))]
    (if (instance? PGPSecretKeyRing
                   keyring)
      (.getSecretKey keyring)
      (.getPublicKey keyring))))

(defn encrypt [message passphrase-or-public-key]
  (message/encrypt
   message
   passphrase-or-public-key
   :format :utf8
   :cipher :aes-256
   :compress :zip
   :armor true))

(defn decrypt-with-passphrase [message password]
  (message/decrypt message
                   password))

(defn decrypt-with-secret-key [message secret-key]
  (message/decrypt message
                   (pgp/unlock-key secret-key
                                   "")))

(defn decrypt [message password-or-secret-key]
  (if (string? password-or-secret-key)
    (decrypt-with-passphrase message password-or-secret-key)
    (decrypt-with-secret-key message password-or-secret-key)))

(deftest test-encrypt
  (is (= "foo" (decrypt (encrypt "foo" "bar")
                        "bar"))))

(defn key-user-id [key]
  (first (:user-ids (pgp/key-info key))))

(defn fingerprint [key]
  (:fingerprint (pgp/key-info key)))

(comment

  (def secret-key (generate-secret-key "key"))

  (key-user-id secret-key)
  (pgp/key-info (decode (encode secret-key)))
  (pgp/key-info (decode (encode (public-key secret-key))))

  (decrypt (encrypt "foo" (public-key secret-key))
           secret-key)

  (decrypt (encrypt "foo" "bar")
           "bar")
  (def encrypted (encrypt "foo" "bar"))
  (time (encrypt "foo" "bar"))

  (time (try (decrypt encrypted "foo")
             (catch Exception e)))
  )

(defn spit-secret-key [id secret-key-file-name]
  (->> (generate-secret-key id)
       (encode)
       (spit secret-key-file-name)))

(defn spit-public-key [secret-key-file-name public-key-file-name]
  (->> (slurp secret-key-file-name)
       (decode)
       (public-key)
       (encode)
       (spit public-key-file-name)))

(defn make-directories [path]
  (.mkdirs (io/file path)))

(defn make-keep [name keep-path public-key-path]
  (make-directories keep-path)
  (let [secret-key-file (io/file keep-path (str name ".secret"))]
    (spit-secret-key name secret-key-file)
    (spit-public-key secret-key-file
                     (io/file public-key-path (str name ".public")))))

(defn make-master-keep [path]
  (make-directories path)
  (spit-secret-key "master"
                   (io/file path "master.secret")))

(comment
  (make-keep "key-1" "temp" "temp")
  (make-keep "1" "temp/1" "temp/laptop")
  (make-keep "2" "temp/2" "temp/laptop")
  (make-keep "3" "temp/3" "temp/laptop")
  (make-master-keep "temp/laptop")
  )

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

(comment
  (create-password-hash-file "temp/laptop/password")
  (ask-and-check-password "temp/laptop/password")
  )

(defn load-master-file [file-name secret-key password]
  (if (.exists (io/file file-name))
    (-> (slurp file-name)
        (decrypt password)
        (decrypt secret-key))
    ""))

(defn save-master-file [contents file-name secret-key password]
  (spit file-name
        (-> contents
            (encrypt secret-key)
            (encrypt password))))

(defn key-combination-file-name [master-file-path public-keys]
  (str master-file-path "_" (apply str (interpose "-" (map key-user-id public-keys)))))

(defn encrypt-with-key-combination [contents public-keys]
  (reduce (fn [data public-key]
            (encrypt data
                     public-key))
          contents
          public-keys))

(defn load-key [path]
  (decode (slurp path)))

(defn save-file-encrypted-with-key-combination [contents master-file-path public-keys]
  (spit (key-combination-file-name master-file-path public-keys)
        (encrypt-with-key-combination contents
                                      public-keys)))

(defn decrypt-with-key-combination [data private-keys]
  (reduce (fn [data private-key]
            (decrypt data
                     private-key))
          data
          private-keys))

(comment
  (decrypt-with-key-combination (encrypt-with-key-combination "foo" (map load-key ["temp/laptop/1.public"
                                                                                   "temp/laptop/2.public"]))
                                (map load-key ["temp/2/2.secret"
                                               "temp/1/1.secret"]))

  (save-file-encrypted-with-key-combination "foo"
                                            "temp/laptop/foo"
                                            (map load-key ["temp/laptop/1.public"
                                                           "temp/laptop/2.public"]))

  (decrypt-with-key-combination (slurp "temp/laptop/foo-1-2")
                                (map load-key ["temp/2/2.secret"
                                               "temp/1/1.secret"]))
  )

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
                                     (decode (slurp secret-key-file-name))
                                     password
                                     public-keys
                                     key-combination-size)))

(defn load-file-with-password [file-name password]
  (if (.exists (io/file file-name))
    (-> (slurp file-name)
        (decrypt password))
    ""))

(defn save-file-with-password [file-name contents password]
  (spit file-name
        (encrypt contents password)))

(defn edit-file-with-password [file-name]
  (when-let [password (editor/ask-password)]
    (save-file-with-password file-name
                             (editor/edit (load-file-with-password file-name password))
                             password)))

(defn edit-file-with-password-and-keys [file-name public-key-file-names]
  (when-let [password (editor/ask-password)]
    (let [new-contents (editor/edit (load-file-with-password file-name password))]
      (save-file-with-password file-name
                               new-contents
                               password)
      (save-file-encrypted-with-key-combination new-contents
                                                file-name
                                                (map load-key public-key-file-names)))))

(comment
  (edit-file-with-password-and-keys "temp/secret"
                                    ["temp/key-1.public"])

  (let [passowrd (editor/ask-password)]
    )

  (edit-file-with-password "temp/message")

  (edit-file "temp/secret-message"
             "temp/laptop/local.secret"
             "temp/laptop/password")

  (-> (slurp "temp/secret-message")
      (decrypt "foo")
      (decrypt (decode (slurp "temp/laptop/local.secret"))))
  )

(defn write-remote-file [contents key-path url file-name]
  (run-command "bash" "-c" (str "ssh -i " key-path " " url " \"cat > " file-name "\"")  :in contents))

(defn read-remote-file [key-path url file-name]
  (run-command "bash" "-c" (str "ssh -i " key-path " " url " \"cat " file-name "\"")))



(comment

  (combinatorics/combinations (range 5)
                              3)

  (def secret (encrypt "this is a secret message this is a secret message this is a secret message" "this is a passphrase"))
  (time (try (decrypt secret "this is a passphrase")
             (catch Exception e)))

  ;; sudo useradd -K PASS_MAX_DAYS=-1 -m secret
  (clj-time/now)
  (clojure.instant/parse-timestamp)
  (pr-str (java.util.Date.))
  (write-remote-file "hello"
                     "/Users/jukka/.ssh/secret"
                     "secret@sirpakauppinen.fi"
                     "greeting.txt")

  (read-remote-file "/Users/jukka/.ssh/secret"
                    "secret@sirpakauppinen.fi"
                    "greeting.txt")

  (.getAbsolutePath (io/file "foo" "bar.txt"))

  (run-command "bash" "-c" "ssh -i /Users/jukka/.ssh/secret secret@sirpakauppinen.fi \"cat > foo.txt\"" :in "foo2\nbar")
  (run-command "bash" "-c" "ssh -i /Users/jukka/.ssh/secret secret@sirpakauppinen.fi cat greeting.txt")
  (run-command "cat" ">" "foo.txt" :in (io/input-stream (.getBytes "foo2")))
  (run-command "cat" ">" "foo.txt" :in (io/input-stream (.getBytes "foo2")))

  (io/make-parents "temp/laptop/foo")
  (io/make-parents "temp/far-keep-1/foo")
  (io/make-parents "temp/far-keep-2/foo")
  (make-directories "temp/cloud")

  (spit-secret-key "temp/laptop/local.private")
  (spit-key-ring "laptop" "temp/laptop" "temp/laptop")
  (spit-key-ring "far-keep-2" "temp/laptop" "temp/far-keep-2")

  (pgp/key-info (:public (generate-keyring "foo")))

  (encode-public-key keyring)
  (spit "keyring.asc" (encode-secret-key keyring))
  (decode-keyring (encode-secret-key keyring))
  (spit "message.asc" (encrypt-with-keyring "foo" keyring))
  (pgp/decode (encrypt-with-keyring "foo" keyring))
  (let [keyring (keyring/list-public-keys (decode-keyring (slurp "temp/laptop/local.private")))]
    keyring
    #_(keyring/list-public-keys (:public keyring))
    #_(decrypt-with-keyring (encrypt-with-keyring "foo" keyring)
                            keyring))

  (scrypt/check "foo" (scrypt/encrypt "foo"))



  {:public-keys #{}
   :private-key nil
   :secrets #{:passwords :second-factories}
   :passwords #{:password}
   :keeps {:eine #{{:secret :passwords
                    :encryptions #{[:leena]
                                   [:password]}}

                   {:secret :second-factories
                    :encryptions #{[:leena]
                                   [:koti]
                                   [:kone]}}}
           :leena #{}
           :koti #{}
           :kone #{}}})
