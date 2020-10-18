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
  (->> [:d0 nil :d1 :d1 :d1 :d1 :d2 :d2 nil nil nil :d1 nil nil :d1]))

(defn bassic2
  []
  (->> [nil nil nil nil :c1 nil nil :c1 nil :c1]))

(defn hh1 []
  (->> (fn [] [nil nil :f#2 (when (> (rand) 0.5)
                              :f#2)])
       (repeatedly 4)
       (apply concat)))

(defn sn1 []
  (->> [nil nil nil nil :c1 nil nil nil ]
       (repeat 2)
       (apply concat)))

(defn snare1 []
  (->> [nil nil nil nil :c1 nil nil nil]
       (repeat 2)
       (apply concat)))

(defn arp1 []
  (->> [:c2 :f2 :g2 :a#2 :c3 :c2 :c3 :a#1]
       (repeat 2)
       (apply concat)))

(defn chords1 []
  [nil nil nil :c1 nil nil :g0 nil nil nil :g0])

(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :loop-length 4
          :channel 2}
         #'chords1))
