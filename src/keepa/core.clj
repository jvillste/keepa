(ns keepa.core
  (:require [clj-pgp.generate :as generate]
            [clj-pgp.core :as pgp]
            [clj-pgp.keyring :as keyring]
            [clj-pgp.message :as message]
            [crypto.password.scrypt :as scrypt]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]))

(defn run-command [& args]
  (let [result (apply shell/sh args)]
    (when (or (not= 0 (:exit result))
              (not= "" (:err result)))
      (throw (ex-info (:err result) result)))
    (:out result)))

#_(def key-pair (generate/generate-keypair (generate/rsa-keypair-generator 3072)
                                           :rsa-general))

(defn generate-keyring [id]
  (generate/generate-keys id ""
                          (master-key (keypair (generate/rsa-keypair-generator 3072)
                                               :rsa-general))))

(defn encode-secret-key [keyring]
  (pgp/encode-ascii (:secret keyring)))

(defn encode-public-key [keyring]
  (pgp/encode-ascii (:public keyring)))

(defn decode-keyring [keyring-data]
  (pgp/decode keyring-data))

(defn encrypt-with-keyring [message keyring]
  (message/encrypt
   message
   (first (keyring/list-public-keys (:secret keyring)))
   :format :utf8
   :cipher :aes-256
   :compress :zip
   :armor true))

(defn decrypt [message keyring]
  (message/decrypt message
                   (pgp/unlock-key (first (keyring/list-secret-keys (:secret keyring)))
                                   "")))
(comment
  (run-command "bash" "-c" "ssh -i /Users/jukka/.ssh/secret secret@sirpakauppinen.fi \"cat > foo.txt\"" :in "foo2\nbar")
  (run-command "bash" "-c" "ssh -i /Users/jukka/.ssh/secret secret@sirpakauppinen.fi cat foo.txt")
  (run-command "cat" ">" "foo.txt" :in (io/input-stream (.getBytes "foo2")))
  (run-command "cat" ">" "foo.txt" :in (io/input-stream (.getBytes "foo2")))
  
  (def keyring (generate-keyring "test-keyring"))
  (encode-public-key keyring)
  (spit "keyring.asc" (encode-secret-key keyring))
  (decode-keyring (encode-secret-key keyring))
  (spit "message.asc" (encrypt-with-keyring "foo" keyring))
  (pgp/decode (encrypt-with-keyring "foo" keyring))
  (decrypt (encrypt-with-keyring "foo" keyring)
           keyring)
  (scrypt/check "foo" (scrypt/encrypt "foo"))

  (message/decrypt (message/encrypt
                    "secret message"
                    "foobar"
                    :format :utf8
                    :cipher :aes-256
                    :compress :zip
                    :armor true)
                   "foobar")

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
