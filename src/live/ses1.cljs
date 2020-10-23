(ns live.ses1
  (:require [live.core :refer [loop! next-beat next-bar repli]]))

(def x nil)

(defn bass1 []
  (->> [:c2 nil nil :c1 nil nil :g1 nil]
       (repeat 2)
       (apply concat)))

(defn bass2 []
  {:sequence (concat [:c1 nil nil :c1 nil nil nil nil]
                     [:d#1 nil nil :d#1 nil nil nil nil]
                     [:f1 nil nil :f1 nil nil nil nil]
                     [:g#1 nil nil :g#1 nil nil {:note 'g1
                                                 :length (/ 1 8)} nil])
   :length (/ 1 16)})

(defn bd1 []
  (->> [:c1 nil nil nil]
       (repeat 4)
       (apply concat)))

(defn bd2 []
  (->> ['c1 x 'c1 x x 'c1 x x 'c1]))

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

(defn bass1 []
  {:sequence [nil nil :c1 nil :c1 nil nil nil :c1 nil nil :c1 nil nil :c1 nil]
   :transpose 0})

(defn hh1 []
  (->> (fn [] [nil nil :f#1 (when (> (rand) 0.5)
                              :f#1)])
       (repli 4)))

(defn sn1 []
  (->> (fn [][nil nil (when (> (rand) 0.9) :d1) nil :d1 nil nil (when (> (rand) 0.7)
                                                                  :d1)])
       (repli 2)))

(defn arp1 []
  {:sequence (->> [:c3 :f3 :g3 :a#3 :c4 :c3 :c4 :a#2]
                  (repeat 2)
                  (apply concat))
   :transpose 0})

(defn chords1 []
  [nil nil nil :c1 nil nil :g0 nil nil nil :g0])

(defn chords2 []
  (->> [nil [:c4 :d#4] nil nil nil]
       (repli 4)))

(defn arp2 []
  (->> [:g3 nil 'd4 nil :c3 nil nil :c4]
       (repeat 2)
       (apply concat)))

(defn melody1 []
  {:length (/ 1 4)
   :step-length (/ 1 8)
   :sequence [[:d#3 :g3] x x [:d3 :f3] x x ['a#2 :d3] x ['c3 :d#3] x x ['g2 'c3]]})


(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :loop-length 8
          :channel 15}
         #'bass2)

  (deref live.core/!app-state)

  (live.core/stop-all!)

  (live.core/stop-matching! {:function 'bd1})

  (live.core/stop-matching! {:channel 15}))
