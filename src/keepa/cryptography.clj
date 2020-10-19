(ns keepa.cryptography
  (:require [clj-pgp.core :as pgp]
            [clj-pgp.generate :as generate]
            [clojure.test :refer :all]
            [clj-pgp.message #_keepa.message :as message])
  (:import [org.bouncycastle.openpgp PGPSecretKey PGPSecretKeyRing]))

(extend-protocol pgp/Encodable

  PGPSecretKey

  (encode [secretkey]
    (.getEncoded secretkey)))

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
   :format (if (string? message)
             :utf8
             :binary)
   :cipher :aes-256
   :compress :zip
   :armor true))

(defn- decrypt-with-passphrase [message password]
  (message/decrypt message
                   password))

(defn- decrypt-with-secret-key [message secret-key]
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

(defn user-id [key]
  (first (:user-ids (pgp/key-info key))))

(defn fingerprint [key]
  (:fingerprint (pgp/key-info key)))



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

(deftest test-encryption
  (let [test-secret-key (generate-secret-key "key")]
    (is (= "key" (user-id test-secret-key)))

    (is (= (fingerprint test-secret-key)
           (fingerprint (public-key test-secret-key))
           (fingerprint (decode (encode test-secret-key)))))

    (let [message "secret message"]
      (= message
         (decrypt (encrypt message (public-key test-secret-key))
                  test-secret-key))

      (let [password "password"]
        (= message
           (decrypt (encrypt message password)
                    password))))))
