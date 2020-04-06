(ns example.new)

(def bpm 117)

(def bps (/ bpm 60))

(def n-steps 8)

(def steps-per-beat 4)

(defn step-no->time [n]
  (* (/ 1 steps-per-beat bps) n))

(defn time->next-step [t]
  (-> (* bps t)
      js/Math.ceil))

(defn s []
      ['c2 'd2 'e2 'f2 'g2 'a2 'b2 'c3])

(def s' (->> (repeatedly #'s)
             (apply concat)))

(defn play [t v]
  (js/console.log 't t 'v v))

(def look-ahead 0.5)

(defonce !start (atom false))

(defn start []
  (reset! !start true)
  (let [start-t (js/performance.now)
        f (fn f [last-t es]
            (println 'last-t last-t 'i (time->next-step last-t))
            (let [now (/ (- (js/performance.now) start-t)
                         1000)
                  notes (->> es
                             (take-while (fn [[idx _]]
                                           (< (step-no->time idx) (+ now look-ahead)))))]
              (println 'now now 'notes notes)
              (when @!start
                (js/setTimeout #(f now (drop (count notes) es))
                               (* 1000 look-ahead)))))]
    (f 0 (map vector (range) s'))))


