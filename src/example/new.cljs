(ns example.new)

(def bpm 120)

(def bps (/ bpm 60))

(def n-steps 8)

(def steps-per-beat)

(defn step-no->time [n]
  (* (/ 1 steps-per-beat bps) n))

(defn time->next-step [t]
  (-> (* bps t)
      js/Math.ceil))

(def s ['c2 'd2 'e2 'f2 'g2 'a2 'b2 'c3])

(def s' (->> (repeatedly (fn []
                           s))
             (apply concat)))

(defn play [t v]
  (js/console.log 't t 'v v))

(def look-ahead 0.1)

(defonce !start (atom false))

(defn start []
  (reset! !start true)
  (let [start-t (js/performance.now)
        f (fn f [last-t]

            (let [i (time->next-step last-t)
                  _ (println 'last-t last-t 'i i)
                  now (/ (- (js/performance.now) start-t)
                         1000)
                  subsec (->> s'
                              (subvec (mapv (fn [v idx]
                                              [v (step-no->time idx)])
                                            s (range))
                                      i)
                              (take-while (fn [[_ t]]
                                            (< t (+ now look-ahead)))))]
              (println 'now now 'subsec subsec)
              (when @!start
                (js/setTimeout #(f now) (* 1000 look-ahead)))))]
    (f 0)))


