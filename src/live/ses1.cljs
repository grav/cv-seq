(ns live.ses1
  (:require [live.core :refer [loop! next-beat next-bar repli]]))

(defn bass1 []
  (->> [:c2 nil nil :c1 nil nil :g1 nil]
       (repeat 2)
       (apply concat)))

(defn bd1 []
  (->> [:c1 nil nil nil]
       (repeat 4)
       (apply concat)))

(defn perc1 []
  (->> [[:a#1 :d3] nil {:note :g1 :length (/ 1 4)}  nil :c1 nil nil :c1 nil nil :c2 nil nil nil :c3]))


(defn bassic1
  []
  (->> [:d0 nil :d1 :d1 :d1 :d1 :d2 :d2 nil nil nil :d1 nil nil :d1]))

(defn bassic2
  []
  (->> [nil nil nil nil :c2 nil nil :c2 nil :c2]))

(defn bassic3 []
  [nil nil :d2 nil :d2 nil nil nil nil :f3 nil nil :d2 nil nil :d3])


(defn hh1 []
  (->> (fn [] [nil nil :f#1 (when (> (rand) 0.5)
                              :f#1)])
       (repli 4)))

(defn sn1 []
  (->> [nil nil (when (> (rand) 0.2) :d2) nil :d1 nil nil (when (> (rand) 0.7)
                                                           :d2)]
       (repeat 2)
       (apply concat)))

(defn arp1 []
  (->> [:c3 :f3 :g3 :a#3 :c4 :c3 :c4 :a#2]
       (repeat 2)
       (apply concat)))

(defn chords1 []
  [nil nil nil :c1 nil nil :g0 nil nil nil :g0])

(defn arp2 []
  (->> [:g3 nil 'd4 nil :c3 nil nil :c4]
       (repeat 2)
       (apply concat)))

(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :loop-length 4
          :channel 11}
         #'perc1))

(comment
  (live.core/stop-all!)

  (live.core/stop-matching! {:function 'perc1})

  (live.core/stop-matching! {:channel 10}))
