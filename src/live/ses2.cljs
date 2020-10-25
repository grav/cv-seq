(ns live.ses2
  (:require [live.core :refer [loop! next-beat next-bar repli]]))

(def x nil)

(def !key :c)

(def !scale :minor)

(def !scale-transpose 0)

(defn arp1 []
  {:sequence [13 33 53 33 53 14]
   :key !key
   :scale-transpose !scale-transpose
   :scale !scale
   :length (/ 1 12)})

(defn arp2 []
  {:sequence [33 53 73 14 34 54]
   :key !key
   :scale-transpose !scale-transpose
   :scale !scale})

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

(defn scale []
  {:scale !scale
   :key !key
   :scale-transpose !scale-transpose
   :sequence [13 23 33 43 53 63 73 83]})


(comment

  ;; modulate the cc#1 with a sine wave
  (let [juno-mod-cc# 01
        juno-channel 11
        now (.now js/performance)
        n 1000 ;; resolution
        t 4000 ;; time (ms)
        delay 1000 ;; delay (ms)
        xs (->> (range n)
                (map #(/ % n)))
        ys (for [i xs]
             (js/Math.sin (* 2 js/Math.PI i) n))
        ys' (map #(int (* 63.5 (+ 1 %))) ys)
        xs' (map #(* % t) xs)
        output (:output @live.core/!app-state)]
    xs' #_(map vector xs ys')
    (doseq [[x y] (map vector xs' ys')]
      (.send output
             #js[(+ live.core/control-change (dec juno-channel)) juno-mod-cc# y]
             (+ delay x now))))

  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :step-length (/ 1 12)
          :loop-length 2
          :channel 11}
         #'arp2)

  (deref live.core/!app-state)

  (live.core/stop-all!)

  (live.core/stop-matching! {:function 'bd1})

  (live.core/stop-matching! {:channel 15}))
