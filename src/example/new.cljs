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

(defonce !state (atom nil))

(defn setup-ctx! []
  (let [{:keys [ctx]} @!state]
    (when ctx
      (.close ctx)))

  (let [ctx (js/window.AudioContext.)
        osc (.createOscillator ctx) #_(.createConstantSource ctx)
        _ (set! (.-type osc) "sine")
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


(def ding
  (fn [[_ _ v t]]
    (let [{:keys [ctx osc gain]} @!state
          #_#_n (* 10 v)]138.59
      (.setValueAtTime (.-frequency osc)
                       (-> (js/Math.pow 2 (/ v 12))
                           (* 330))
                       t)
      (println 'current-time (.-currentTime ctx) 't t)
      (.setValueAtTime (.-gain gain) 0.0 t)
      (.linearRampToValueAtTime (.-gain gain) 0.5 (+ t 0.01))
      (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t 0.1))
      #_(.setValueAtTime (.-gain))
      #_(print v t))))

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
              #_(doseq [n notes]
                  (ding))
              (when @!start
                (js/setTimeout #(f now (drop (count notes) es))
                               (* 1000 look-ahead)))))]
    (f 0 (map vector (range) s'))))


