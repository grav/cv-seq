(ns example.seq)

(defn secs-per-tick
  [bpm]
  (/ (/ 1 (/ bpm 60)) 4))

(defn- next-notes [latency {:keys [sequence transpose] :or {transpose 0}} beat now time spt]
  (->> sequence
       ;; TODO - doesn't belong here - it's for transposing?
       (map (fn [notes] (->> notes
                             (map (fn [{:keys [note]
                                        :as   v}]
                                    (assoc v :note (+ transpose note)))))))
       (repeat)
       (apply concat)
       (drop (mod beat (count sequence)))
       (map-indexed (fn [i v] [(+ time (* i spt)) v]))
       (take-while (fn [[t _]] (< t (+ now (* 1.5 latency)))))))

(defn debug [v]
  (println 'debug v)
  v)

(defn play-sequences! [{:keys [latency bpm sequences now beat time]}]
  ":notes - notes to be immediately queued up
   :beat :time - pointers to next "
  (let [spt (secs-per-tick bpm)
        sequences (for [{:keys [device sequence]} sequences
                        :when (:sequence sequence)]
                    {:device  device
                     :notes   (next-notes latency sequence beat now time spt)
                     :channel (or (:channel sequence) 0)})

        diff (- now time)
        c (max (int (/ diff spt)) (or (->> (map count (map :notes sequences))
                                           (apply max))
                                      0))
        beat' (+ c beat)
        time' (+ (* spt c) time)]
    {:beat beat'
     :time time'
     :sequences sequences}))

(def latency 0.1)

#_(defn ding [args]
    (print 'ding args))

(defn play-repeatedly
  [{:keys [now-fn on-update-beat !sequences seq-transform ding]} {:keys [beat time]}]
  (let [bpm 110
        {:keys [position sequences]
         :as res} (play-sequences! {:latency latency
                                    :bpm bpm
                                    :sequences (seq-transform @!sequences)
                                    :now (/ (now-fn) 1000)
                                    :beat beat
                                    :time time})]
    (doseq [n (for [{:keys [device notes channel]} sequences
                    [i vs] notes
                    {:keys [note sustain]} vs]
                [device channel note #_(+ 0x24 note) i sustain])]
      (ding n))
    (on-update-beat beat)
    (js/setTimeout #(play-repeatedly {:now-fn now-fn
                                      :ding ding
                                      :!sequences !sequences
                                      :seq-transform seq-transform
                                      :on-update-beat on-update-beat}
                                     res)
                   (* latency 1000))))

