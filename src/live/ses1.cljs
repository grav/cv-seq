(ns live.ses1
  (:require [live.core :refer [loop! next-beat next-bar]]))

(defn bass1 []
  (->> [:c2 nil nil :c1 nil nil :g1 nil]
       (repeat 2)
       (apply concat)))

(defn bd1 []
  (->> [:c1 nil nil nil]
       (repeat 4)
       (apply concat)))

(defn bassic1
  []
  (->> [nil nil :c1 :c1 :c1 :c1 :c2 :c2]))

(defn bassic2
  []
  (->> [nil nil nil nil :c1 nil nil :c1 :nil :c1]))

(defn hh1 []
  (->> [nil nil :c1 nil]
       (repeat 4)
       (apply concat)))


(defn snare1 []
  (->> [nil nil nil nil :c1 nil nil nil]
       (repeat 2)
       (apply concat)))

(defn arp1 []
  (->> [:c2 :f2 :g2 :a#2 :c3 :c2 :c3 :a#1]
       (repeat 2)
       (apply concat)))

(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :loop-length 4
          :channel 6}
         #'bd1))
