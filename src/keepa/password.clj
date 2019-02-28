(ns keepa.password
  (:import java.security.SecureRandom))

(def ^:private secure-random (SecureRandom.))

(defn secure-rand-int [max]
  (.nextInt secure-random max))

(defn secure-rand-nth [coll]
  (nth coll (secure-rand-int (count coll))))

(defn generate-password [length]
  (apply str (take length (repeatedly (fn [] (secure-rand-nth (map char (concat (range 97 123)
                                                                                (range 65 91)
                                                                                (range 48 58)))))))))

(comment
  (generate-password 30)

  )
