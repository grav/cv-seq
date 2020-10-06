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
     origin))

(comment
  (let [channel 0]
           (.send @!output #js[(+ channel note-on)
                               42
                               0x7f]
                  (next-beat 120))))
