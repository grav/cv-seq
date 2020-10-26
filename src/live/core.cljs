(ns live.core
  (:require [clojure.string :as str]
            [cljs-bean.core :refer [bean]]
            [reagent.core :as r]
            [clojure.pprint]))

(def output-name-prefix "Synth input port")

(def note-on 0x90)

(def note-off 0x80)

(def vol 0x7f)

(def control-change 0xb0)

(def all-off
  (for [channel (range 16)
        note (range 128)]
    {:type :note-off
     :data1 note
     :data2 0
     :channel (inc channel)}))

(defonce !app-state (r/atom {:tempo 120
                             :output nil}))

(defonce origin (.now js/performance))

(defn get-all-outputs []
  (-> (.requestMIDIAccess js/navigator)
      (.then (fn [m]
               (->> m
                    (.-outputs)
                    (.values)
                    es6-iterator-seq)))))

(defn get-output-p [prefix]
  (-> (get-all-outputs)
      (.then (fn [devices]
               (->> devices
                    (filter #(str/starts-with? (.-name %) prefix))
                    first)))))

(defn duration []
  (/ (- (.now js/performance) origin)
     1000))

(defn current-beat [bpm]
  (-> (duration)
      (/ 60)
      (* bpm)
      int))

(def latency 0.5)


(defn bpm->secs-per-beat [bpm]
  (/ 60 bpm))

(defn next-bar [bpm beats-per-bar]
  (let [current-b (current-beat bpm)
        m (mod current-b beats-per-bar)
        n (- beats-per-bar m)]
    (+ (* current-b
          (bpm->secs-per-beat bpm))
       (* n (bpm->secs-per-beat bpm))
       (/ origin 1000))))

(defn next-beat [bpm]
  (next-bar bpm 1))

(defn note->step [[a b]]
  (cond-> (get {"c" 0 "d" 2 "e" 4 "f" 5 "g" 7 "a" 9 "b" 11} a)
    (= b "#") inc
    (= b "b") dec))

(defn try-parse-int [s]
  (let [r (js/parseInt s)]
    (when-not (js/isNaN r)
      r)))

(defn note->octave [note]
 (-> (last note)
     try-parse-int))

(def major [1 3 5 6 8 10 12])

(def minor [1 3 4 6 8 9 11])

(def scales
  {:minor minor
   :dorian [1 3 4 6 8 10 11]
   :ionian major
   :major major
   :frygian [1 3 5 7 8 10 12]})

(def key->offset
  {"c" 0 "c#" 1 "db" 1 "d" 2 "d#" 3 "eb" 3 "e" 4 "fb" 4 "e#" 5 "f" 5 "f#" 6 "gb" 6 "g" 7 "g#" 8 "ab" 8 "a" 9 "a#" 10 "bb" 10 "b" 11 "cb" 11})

(defn scale-step->step [scale-step {:keys [scale key scale-transpose]}]
  (assert (and scale) "scale and key required for scale-step->step")
  (let [step (-> scale-step
                 (+ (or scale-transpose) 0)
                 dec)]

    (-> (nth (get scales scale) (mod step 7))
        dec
        ;; we may have increased in octaves - add these
        (+ (* 12 (bit-shift-left (/ step 7) 0)))
        (+ (key->offset (name key))))))

(defn note->midi-message [{:keys [note
                                  midi
                                  length
                                  velocity
                                  offset
                                  tempo
                                  sustain
                                  transpose
                                  channel] :as args}]
  (let [secs-per-beat (bpm->secs-per-beat tempo)
        note-str (if (integer? note)
                   (str note)
                   (name note))
        step (if (try-parse-int (first note-str))
               (scale-step->step (js/parseInt (first note-str)) args)
               (note->step note-str))
        octave (note->octave note-str)
        note-val (+ (* 12 (+ 2 octave))
                    step)]
    (if midi
      (let [[type data1 data2] midi]
        [{:type type
          :data1 data1
          :data2 data2}])
      [{:type :note-on
        :time offset
        :data1 (+ note-val transpose)
        :data2 (int (* (or velocity 0.8) 128))
        :channel channel}
       {:type :note-off
        :time (+ offset
                 (* (or length (/ 1 16))
                    4
                    secs-per-beat (or sustain 0.9)))
        :data1 (+ note-val transpose)
        :data2 0
        :channel (or channel 1)}])))

(comment
  (note->octave  "c3")

  (note->midi-message {:note :c3
                       :offset 0}))

(def beats-per-bar 4)

(defn sequence->notes [{:keys [tempo] :as args} seq]
  (assert (and tempo) "Must set tempo!")
  (let [{:keys [sequence step-length] :as sequence-params} (if (map? seq) seq {:sequence seq})]
    (->> sequence
         (reduce (fn [{:keys [notes offset] :as _args :or {offset 0}}  n]
                   {:notes (concat notes
                                   (->> (if (sequential? n) n [n])
                                        (remove nil?)
                                        (map #(note->midi-message (merge
                                                                   args
                                                                   {:offset offset}
                                                                   sequence-params
                                                                   (if (map? %) % {:note %}))))
                                        (apply concat)))
                    :offset (+ (* (bpm->secs-per-beat tempo)
                                  (or step-length
                                      (:step-length args)
                                      (/ 1 16))
                                  beats-per-bar)
                               offset)})
                 nil)
         :notes)))

(comment
  (sequence->notes
   {:tempo 120
    :offset 0
    :channel 10} [:c4]))

(defn play-notes! [{:keys [offset output]} notes]
  (doseq [{:keys [type data1 data2 time channel]} notes]
    (let [status (+ (get {:note-on note-on
                          :note-off note-off}
                         type)
                    (dec channel))]
      (.send (or output (:output @!app-state))
             #js[status data1 data2]
             (* 1000 (+ offset time))))))


#_(defn schedule! [notes-fn offset]
    (let [now (js/performance.now)
          delay (max 0 (- (* 1000 offset) now (* 1000 latency)))]
      (println 'delay delay)
      (js/setTimeout (fn []
                       (play-notes! {} (notes-fn offset)))
                     delay)))

(defn loop! [{:keys [offset tempo loop-length id channel] :as args
              :or {id (random-uuid)}} notes-fn-var]
  (assert (var? notes-fn-var) "Supplied function needs to be a var")
  (let [now (js/performance.now)
        delay (max 0 (- (* 1000 offset) now (* 1000 latency)))
        next-offset (+ offset
                       (* (bpm->secs-per-beat tempo) loop-length))]
    (swap! !app-state assoc-in [:sequences id]
           {:channel channel
            :function (:name (meta notes-fn-var))
            :callback-id (js/setTimeout (fn []
                                          (try (play-notes! args
                                                            (sequence->notes args (notes-fn-var)))
                                               (catch js/Error e
                                                   (println 'boom e)))
                                          (loop! (assoc args :offset next-offset :id id)
                                                 notes-fn-var))
                                        delay)})))


;;; fluidsynth
;;; fluidsynth -a pulseaudio -m alsa_seq -l /usr/share/soundfonts/freepats-general-midi.sf2

(defn init []
  (-> (get-output-p "TORAIZ")
      (.then #(swap! !app-state assoc :output %))))


(defn stop-sequence! [id]
  (let [{:keys [callback-id]} (get-in @!app-state [:sequences id])]
    (js/clearTimeout callback-id)
    (swap! !app-state update :sequences dissoc id)))

(defn stop-all! []
  (doseq [[id _] (:sequences @!app-state)]
    (stop-sequence! id)))

(defn repli
  [n es]
  (->> es
       ((if (fn? es) repeatedly repeat) n)
       (apply concat)))

(defn stop-matching! [{:keys [channel function]}]
  (doseq [[id {c :channel f :function}] (:sequences @!app-state)
          :when (and (or (nil? channel)
                         (= channel c))
                     (or (nil? function)
                         (= function f)))]
    (stop-sequence! id)))
