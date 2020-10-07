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


(defn bpm->secs-per-beat [bpm]
  (/ 60 bpm))

(defn next-bar [bpm beats-per-bar]
  (let [current-b (current-beat bpm)
        m (mod current-b beats-per-bar)
        n (if (zero? m) beats-per-bar
                        (- beats-per-bar m))]
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
                                  sustain]}]
  (let [secs-per-beat (bpm->secs-per-beat tempo)
        note-val (note->data1 note)]
    [{:type :note-on
      :time offset
      :data1 note-val
      :data2 (int (* (or velocity 0.5) 128))}
     {:type :note-off
      :time (+ offset
               (* (or length (/ 1 16)) 4 secs-per-beat (or sustain 0.9)))
      :data1 note-val
      :data2 0}]))

(defn sequence->notes [seq {:keys [length velocity tempo offset]}]
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
                                                          :offset offset'})))
                    :offset (+ (/ (bpm->secs-per-beat tempo)
                                  4)
                               offset')}))
               nil)
       :notes))

(defn play-notes [notes]
  (let [channel 0]
    (doseq [{:keys [type data1 data2 time]} notes]
      (let [status (+ (get {:note-on note-on
                            :note-off note-off}
                           type)
                      channel)]
        (.send @!output #js[status data1 data2]
                     (* 1000 time))))))

(comment
  (-> [:c2 :c2 :e2 :c2 :g2 nil nil :g2 :c3 :c3 :b2 :a2 :g2]
      (sequence->notes {:tempo 120
                        :offset (next-bar 120 4)})
      play-notes))

;;; fluidsynth
;;; fluidsynth -a pulseaudio -m alsa_seq -l /usr/share/soundfonts/freepats-general-midi.sf2