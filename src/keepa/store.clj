(ns keepa.store
  (:require [keepa.password :as password]
            [keepa.shell :as shell]
            [clj-http.client :as client]
            [hickory.core :as hickory]))

(def ^:private htpassword-directory "/var/www/htpasswd")
(def ^:private public-directory "/var/www/html/public")

(defn- htaccess-file [htpasswd-directory store-name]
  (str "Options +Indexes

AuthType Basic
AuthName \"Authentication Required\"
AuthUserFile \"" htpassword-directory "/" store-name "\"
Require valid-user

Order allow,deny"))

(defn set-password [server store-name password]
  (shell/run-command "ssh" server (str "htpasswd -c -i " htpassword-directory "/" store-name " user") :in password))

(defn write-remote-file [contents server file-name]
  #_(shell/run-command "bash" "-c" (str "ssh " server " \"cat > " file-name "\"")  :in contents)
  (shell/run-command "ssh" server (str "cat > " file-name)  :in contents))

(defn read-remote-file [key-path server file-name]
)

(defn make-remote-directory [server directory-path]
  (shell/run-command "ssh" server (str "mkdir " directory-path)))

(comment
  (make-remote-directory "sirpakauppinen.fi" (str public-directory "/foo"))
  (write-remote-file (htaccess-file htpassword-directory
                                    "foo")
                     "sirpakauppinen.fi"
                     (str public-directory "/" "foo" "/" ".htaccess"))
  )

(defn create-store [server public-directory htpassword-directory]
  (let [store-name (password/generate-password 20)
        password (password/generate-password 20)]
    (make-remote-directory server
                           (str public-directory "/" store-name))
    ))

(defn- file-name-from-tr [tr]
  (get-in tr [3 2 2]))

(defn- file-names-from-index-html [html-string]
  (-> (hickory/as-hiccup (hickory/parse html-string))
      second
      (get-in [4 5 3])
      (as-> x
          (drop 8 x)
          (filter vector? x)
          (map file-name-from-tr x)
          (remove nil? x))))

(defn list-files [url username password]
  (file-names-from-index-html (:body (client/get url {:basic-auth [username password]}))))
