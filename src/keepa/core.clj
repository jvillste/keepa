(ns keepa.core
  (:require [clj-pgp.generate :as generate]
            [clj-pgp.core :as pgp]
            [clj-pgp.keyring :as keyring]
            [clj-pgp.message :as message]
            [crypto.password.scrypt :as scrypt]
            [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clj-time.core :as clj-time]
            [clj-time.format :as format]
            [clojure.math.combinatorics :as combinatorics]
            [keepa.editor :as editor])
  (:import [org.bouncycastle.openpgp PGPSecretKey]))


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

(defn generate-secret-key []
  (.getSecretKey (:secret (generate/generate-keys "" ""
                                                  (master-key (keypair (generate/rsa-keypair-generator 3072)
                                                                       :rsa-general))))))

(defn public-key [secret-key]
  (.getPublicKey secret-key))

(defn encode [secret-or-public-key]
  (pgp/encode-ascii secret-or-public-key))

(defn decode-secret-key [secret-key-data]
  (.getSecretKey (first (pgp/decode secret-key-data))))

(defn decode-public-key [public-key-data]
  (.getPublicKey (first (pgp/decode public-key-data))))

(defn encrypt [message passphrase-or-public-key]
  (message/encrypt
   message
   passphrase-or-public-key
   :format :utf8
   :cipher :aes-256
   :compress :zip
   :armor true))

(defn decrypt-with-passphrase [message passphrase]
  (message/decrypt message
                   passphrase))

(defn decrypt-with-secret-key [message secret-key]
  (message/decrypt message
                   (pgp/unlock-key secret-key
                                   "")))

(defn decrypt-with-keyring [message keyring]
  (message/decrypt message
                   (pgp/unlock-key (first (keyring/list-secret-keys (:secret keyring)))
                                   "")))


(comment
  (def secret-key (generate-secret-key))

  (pgp/key-info (decode-secret-key (encode-secret-key secret-key)))
  (pgp/key-info (decode-public-key (encode-public-key secret-key)))

  (decrypt-with-secret-key (encrypt "foo" (public-key secret-key))
                           secret-key)

  (decrypt-with-passphrase (encrypt "foo" "bar")
                           "bar"))

(defn spit-secret-key [file-name]
  (->> (generate-secret-key "key")
       (encode)
       (spit file-name)))

(defn make-directories [path]
  (.mkdirs (io/file path)))

(defn ask-and-check-password [file-name]
  (let [password (editor/ask-password)]
    (if (scrypt/check password
                      (slurp file-name))
      password
      (do (println "Invalid password")
          nil))))

(comment
  (ask-and-check-password "temp/laptop/password")
 )

(defn edit-file [file-name keyring passphrase]
  (let [save (fn [text]
               (spit file-name
                     (-> text
                         (encrypt keyring)
                         (encrypt passphrase))))]
    (if (.exists (io/file file-name))
      (-> (slurp file-name)
          (decrypt-with-passphrase passphrase)
          (decrypt-with-keyring keyring)
          (editor/edit save))
      (editor/edit ""
                   save))))

(comment
  (edit-file "temp/secret-message"
             (decode-keyring (slurp "temp/laptop/local.private"))
             (ask-and-check-password "temp/laptop/password")))

(defn write-remote-file [contents key-path url file-name]
  (run-command "bash" "-c" (str "ssh -i " key-path " " url " \"cat > " file-name "\"")  :in contents))

(defn create-password [file-name]
  (spit file-name (scrypt/encrypt (editor/ask-password))))

(comment

  (create-password "temp/laptop/password")

  (combinatorics/combinations (range 4)
                              2)

  ;; sudo useradd -K PASS_MAX_DAYS=-1 -m secret
  (clj-time/now)
  (clojure.instant/parse-timestamp)
  (pr-str (java.util.Date.))
  (write-remote-file "hello"
                     "/Users/jukka/.ssh/secret"
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
