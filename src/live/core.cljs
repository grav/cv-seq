(ns live.core
  (:require [clojure.string :as str]
            [cljs-bean.core :refer [bean]]))

(def output-name-prefix "Synth input port")

(def note-on 0x90)

(def note-off 0x80)

(def vol 0x7f)

(defonce !output (atom nil))

(defonce origin (.now js/performance))

(defn save-output []
  (-> (.requestMIDIAccess js/navigator)
      (.then (fn [m]
               (->> m
                    (.-outputs)
                    (.values)
                    es6-iterator-seq
                    (filter #(str/starts-with?
                               (.-name %) output-name-prefix))
                    first)))
      (.then #(reset! !output %))))

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

(defn note->data1 [note]
  (let [[a b c d] (name note)
        oct (-> (cond (and (= b "-")
                           (nil? d))
                      (str b c)
                      c
                      (str c d)
                      :else
                      (str b))
                js/parseInt)
        note-val (cond-> (get {"c" 0 "d" 2 "e" 4 "f" 5 "g" 7 "a" 9 "b" 11} a)
                         (= b "#") inc)]
    (-> (+ 2 oct)
        (* 12)
        (+ note-val))))


(defn note->midi-message [note
                          {:keys [length
                                  velocity
                                  offset
                                  tempo
                                  sustain
                                  channel]}]
  (let [secs-per-beat (bpm->secs-per-beat tempo)
        note-val (note->data1 note)]
    [{:type :note-on
      :time offset
      :data1 note-val
      :data2 (int (* (or velocity 0.5) 128))
      :channel channel}
     {:type :note-off
      :time (+ offset
               (* (or length (/ 1 16)) 4 secs-per-beat (or sustain 0.9)))
      :data1 note-val
      :data2 0
      :channel channel}]))

(defn sequence->notes [{:keys [length velocity tempo offset channel]} seq]
  (assert (and tempo offset) "Must set tempo and offset!")
  (->> seq
       (reduce (fn [{:keys [notes] :as args}  n]
                 (let [offset' (get args :offset offset)]
                   {:notes (concat notes
                                   (when n
                                     (note->midi-message n
                                                         {:length length
                                                          :velocity velocity
                                                          :tempo tempo
                                                          :offset offset'
                                                          :channel channel})))
                    :offset (+ (/ (bpm->secs-per-beat tempo)
                                  4)
                               offset')}))
               nil)
       :notes))

(defn play-notes! [notes]
  (println 'play-notes!)
  (doseq [{:keys [type data1 data2 time channel]} notes]
    (let [status (+ (get {:note-on note-on
                          :note-off note-off}
                         type)
                    (or channel 0))]
      (.send @!output #js[status data1 data2]
             (* 1000 time)))))

(defn schedule! [notes-fn offset]
  (let [now (js/performance.now)
        delay (max 0 (- (* 1000 offset) now (* 1000 latency)))]
    (println 'delay delay)
    (js/setTimeout (fn []
                     (play-notes! (notes-fn offset)))
                   delay)))

(defn loop! [notes-fn {:keys [offset bpm loop-length]}]
  (let [now (js/performance.now)
        delay (max 0 (- (* 1000 offset) now (* 1000 latency)))
        next-offset (+ offset
                       (* (bpm->secs-per-beat bpm) loop-length))]

    (js/setTimeout (fn []
                     (play-notes! (notes-fn offset))
                     (loop! notes-fn
                            {:offset next-offset
                             :bpm bpm
                             :loop-length loop-length}))
                   delay)))

(defn funk [offset]

  (sequence->notes

    {:tempo 120
     :offset offset
     :channel 9}
    (->> [nil nil nil nil :d1 nil nil nil]
         (repeat 2)
         (apply concat))))

(defn funk-clap [offset]
  (sequence->notes
    {:tempo 120
     :offset offset
     :channel 9}
    (->> [[nil nil nil :d#1 nil nil :d#1 nil]
          [nil :d#1 nil :d#1 nil nil :d#1 nil]]
         rand-nth
         (repeat 2)
         (apply concat))))

(defn bd [offset]
  (sequence->notes

    {:tempo 120
     :offset offset
     :channel 9}
    (->> [:c1 nil nil nil]
         (repeat 4)
         (apply concat))))


(defn hihats [offset]

  (sequence->notes

    {:tempo 120
     :offset offset
     :channel 9}
    (->> [:f#1 :f#1 :a#1 :f#1]
         (repeat 4)
         (apply concat))))

(defn bass [offset]
  (sequence->notes
    {:tempo 120
     :offset offset
     :channel 0}
    (apply concat (repeat 2 [:c1 nil nil :c1 nil nil :c1 nil]))))

(defn my-notes [offset]
  (->> [[:c2 :g2 :e2 :c2 :g2 nil nil :g2 :c3 :c3 :b2 :a2 :g2]
        [:g3 nil :f3 :d3 :e3 nil :c3 :d3 :e3 :f3 :d3 :g3 :e3]]
       rand-nth
       (sequence->notes {:tempo 120
                         :offset offset})))

(comment
  (schedule! my-notes (next-bar 120 4)))

(comment
  (loop! #'funk-clap {:offset (next-bar 120 4)
                              :bpm 120
                              :loop-length 4}))

;;; fluidsynth
;;; fluidsynth -a pulseaudio -m alsa_seq -l /usr/share/soundfonts/freepats-general-midi.sf2