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

(defn next-beat [bpm]
  (+ (* (current-beat bpm)
        (bpm->secs-per-beat bpm))
     (bpm->secs-per-beat bpm)
     (/ origin 1000)))

(defn next-bar [bpm beats-per-bar]
  (+ (* (current-beat bpm)
        (bpm->secs-per-beat bpm))
     (bpm->secs-per-beat bpm)
     origin))

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
                                  secs-per-bar
                                  sustain]}]
  (let [note-val (note->data1 note)]
    [{:type :note-on
      :time offset
      :data1 note-val
      :data2 (int (* velocity 128))}
     {:type :note-off
      :time (+ offset
               (* length secs-per-bar sustain))
      :data1 note-val
      :data2 0}]))


(comment
  (let [channel 0]
    (doseq [{:keys [type data1 data2 time]} (note->midi-message
                                              :c2
                                              {:length 0.1
                                               :velocity 0.8
                                               :offset (next-beat 120)
                                               :secs-per-bar (* 4 (bpm->secs-per-beat 120))
                                               :sustain 0.9})]
      (let [status (+ (get {:note-on note-on
                            :note-off note-off}
                           type)
                      channel)]
        (.send @!output #js[status data1 data2]
                     (* 1000 time))))))

;;; fluidsynth
;;; fluidsynth -a pulseaudio -m alsa_seq -l /usr/share/soundfonts/freepats-general-midi.sf2