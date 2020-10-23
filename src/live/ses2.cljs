(ns live.ses2
  (:require [live.core :refer [loop! next-beat next-bar repli]]))

(def x nil)



(defn arp1 []
  {:sequence ['c3 'f3 'g3 'f3 'a#3 'c4]
   :length (/ 1 12)})

(defn bd1 []
  [[:c1 :f#1] x :f#1 [:d1 :c1] :f#1 (when (> (rand) 0.8) :a#1)])

(defn bass1 []
  (concat [:c1 x :c1 x :c1 :g0] [:c1 x :c1 x :c1 :g0]))

(defn melody1 []
  ['d#3 x x x x x 'd3 'd#3 'd3 x x 'a#2 'c3 x x x x x x x x x 'a#2 'g2 'f2 'g2 'a#2 'g2 'f2 x 'd#2 'f2 x 'd2 x 'a#1 'c2])


(defn chords1 []
  [x x x x x x
   ['g3 'c4 'd#4] x ['g3 'c4 'd#4] x x x
   x x x x x x
   ['g3 'c4 'f4] x ['g3 'c4 'f4]])



(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :step-length (/ 1 12)
          :loop-length 8
          :channel 12}
         #'chords1)

  (deref live.core/!app-state)

  (live.core/stop-all!)

  (live.core/stop-matching! {:function 'chords1})

  (live.core/stop-matching! {:channel 15}))
