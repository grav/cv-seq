(ns live.ses3
  (:require [live.core :refer [loop! next-beat next-bar repli]]))

(def x nil)
(def scale-transpose 0)

(def scale :minor)

(def key :c)

(defn arp1 []
  {:scale scale
   :key key
   :scale-transpose scale-transpose
   :transpose 12
   :sequence ['13 '33 '14 '73 ['33 '10] ['23 '10] '52 '23 '33 '73 '23 '33]})

(defn drums1 []
  [{:note :c1 :channel 7} :c1 x :c1 x x :a#1 x   x  {:note :c1 :channel 7} x x x x x 'e#1])

(defn hh1 []
  (cond->> [{:note :f#2 :velocity 0.1} x :f#2 x :f#2 {:velocity 0.2 :note :f#2}]
           (> (rand) 0.2) reverse))

(defn melody1 []
  (when (> (rand) 0.7)
      {:scale scale
       :key key
       :scale-transpose scale-transpose
       :sequence ['11 x x '51 x x '32 '71 '12 '52 '42 '72 '52]}))

(comment

  ;; modulate the cc#1 with a sine wave
  (let [juno-mod-cc# 01
        juno-channel 12
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
    (doseq [[x y] (map vector xs' ys')]
      (.send output
             #js[(+ live.core/control-change (dec juno-channel)) juno-mod-cc# y]
             (+ delay x now))))

  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :step-length (/ 1 12)
          :loop-length 4
          :channel 1}
         #'drums1)

  (deref live.core/!app-state)

  (live.core/stop-all!)

  (live.core/stop-matching! {:function 'arp1})

  (live.core/stop-matching! {:channel 10})

  (live.core/play-notes! {:offset 0} live.core/all-off))




;; => nil
;; => nil
