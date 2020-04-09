(ns example.new)

(def bpm 117)

(def bps (/ bpm 60))

(def steps-per-beat 4)

(defn step-no->time [n]
  (* (/ 1 steps-per-beat bps) n))

(defn time->next-step [t]
  (-> (* bps t)
      js/Math.ceil))

(defn s []
      ['c4 'g3 'e3 'f#3 'g3 'a3 'b3 'c4])

(def s' (->> (repeatedly #'s)
             (apply concat)))

(def look-ahead 0.5)

(defonce !start (atom false))

(defonce !state (atom nil))

(defn setup-ctx! []
  (let [{:keys [ctx]} @!state]
    (when ctx
      (.close ctx)))

  (let [ctx (js/window.AudioContext.)
        osc (.createOscillator ctx) #_(.createConstantSource ctx)
        _ (set! (.-type osc) "square")
        _ (.setValueAtTime (.-frequency osc) 8 (.-currentTime ctx))
        gain (.createGain ctx)
        _ (.setValueAtTime (.-gain gain) 0.5 (.-currentTime ctx))

        scriptNode (.createScriptProcessor ctx 4096 1 1)
        _ (set! (.-onaudioprocess scriptNode) (js/algebra #(+ 0.5 %)))]

    (.connect osc gain)
    (.connect gain scriptNode)
    (.connect scriptNode (.-destination ctx))
    (.start osc)
    (swap! !state assoc :ctx ctx :osc osc :gain gain)))

(def fs
  (->> [130.81  138.59 146.83  155.56 164.81 174.61 185.00 196.00 207.65 220 233.08 246.94]
       (map vector (->> ['c 'c# 'd 'd# 'e 'f 'f# 'g 'g# 'a 'a# 'b ] (map str)))
       (into {})))

(defn note->freq [n]
  (let [s (str n)
        note (if (= 2 (count s))
               (first s)
               (subs s 0 2))
        oct (last s)]
    (-> (get fs note)
        (* (js/Math.pow 2 (- oct 3))))))

(defn step->freq [n]
  (-> (js/Math.pow 2 (/ n 12))
      (* 330)))

(def ding
  (fn [f t]
    (let [{:keys [osc gain]} @!state]
      (.setValueAtTime (.-frequency osc) f t)

      (.setValueAtTime (.-gain gain) 0.0 t)
      (.linearRampToValueAtTime (.-gain gain) 0.5 (+ t 0.01))
      (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t 0.1)))))

(defn start []
  (setup-ctx!)
  (reset! !start true)
  (let [start-t (js/performance.now)
        f (fn f [es]
            (let [now (/ (- (js/performance.now) start-t)
                         1000)
                  notes (->> es
                             (take-while (fn [[idx _]]
                                           (< (step-no->time idx) (+ now look-ahead)))))]
              (doseq [[b n] notes]
                (ding (note->freq n) (step-no->time b)))
              (when @!start
                (js/setTimeout #(f (drop (count notes) es))
                               (* 1000 look-ahead)))))]
    (f (map vector (range) s'))))


