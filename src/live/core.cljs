(ns ^:dev/once live.core
  (:require [clojure.string :as str]
            [cljs-bean.core :refer [bean]]
            [reagent.core :as r]
            [clojure.pprint]
            [live.views :as views]))

(def output-name-prefix "Synth input port")

(def note-on 0x90)

(def note-off 0x80)

(def vol 0x7f)

(defonce !app-state (r/atom {:tempo 120
                             :output nil}))

(defonce origin (.now js/performance))

(defn get-output-p [prefix]
  (-> (.requestMIDIAccess js/navigator)
      (.then (fn [m]
               (->> m
                    (.-outputs)
                    (.values)
                    es6-iterator-seq
                    (filter #(str/starts-with?
                               (.-name %) prefix))
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
      :data2 (int (* (or velocity 0.8) 128))
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
                                                          :channel (or channel 0)})))
                    :offset (+ (/ (bpm->secs-per-beat tempo)
                                  4)
                               offset')}))
               nil)
       :notes))

(defn play-notes! [{:keys [tempo offset channel output] :as args} notes]
  (doseq [{:keys [type data1 data2 time channel]} (sequence->notes
                                                    {:tempo tempo
                                                     :offset offset
                                                     :channel channel}
                                                    notes)]
    (let [status (+ (get {:note-on note-on
                          :note-off note-off}
                         type)
                    (or channel 0))]
      (.send (or output (:output @!app-state))
             #js[status data1 data2]
             (* 1000 time)))))


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
                                          (play-notes! args
                                                       (notes-fn-var))
                                          (loop! (assoc args :offset next-offset :id id)
                                                 notes-fn-var))
                                        delay)})))

(defn funk []
  (->> (fn [] [nil nil nil nil :d1 nil nil (rand-nth [nil :d1])])
       (repeatedly 2)
       (apply concat)))


(defn funk-clap []
  (->> [[nil nil nil :d#1 nil nil :d#1 nil]
        [:a#0 nil nil :d#1 nil nil :d#1 nil]]
       rand-nth
       (repeat 2)
       (apply concat)))

(defn pad []
  [nil nil (rand-nth [:c1 :g0])])

(defn funk-clap2 []
  [] #_(->> [[nil nil :d#1 :d#1 nil nil :d#1 nil]
             [:d#1 :d#1 nil :d#1 nil nil :d#1 nil]]
            rand-nth
            (repeat 2)
            (apply concat)))

(defn bd []
  (->> [:c1 nil nil :c1 nil nil :c1 nil nil nil nil :c1 nil nil :c1]))

(defn bd2 []
  (->> [:c1 nil nil nil]
       (repeat 5)
       (apply concat)))


(defn hihats []
  (->> (fn [] [nil nil :c#1 nil])
       (repeatedly 4)
       (apply concat)))

(defn bass []
  (apply concat (repeat 2 [:c1 :c1 nil :c1 nil nil :c1 nil])))

(defn chord1 []
  [:d#5 nil nil :d#5 nil nil :d#5 nil nil :d#5 nil nil :d#5 nil :f5 nil])

(defn chord2 []
  [:g4 nil nil :g4 nil nil :g4 nil nil :g4 nil nil :g4 nil :a#4 nil])

(defn chord3 []
  [:c5 nil nil :c5 nil nil :c5 nil nil :c5 nil nil :c5 nil :c5 nil])

(defn snare []
  (concat (repeat 4 nil) [:c1]))

(defn my-notes []

  (->> [[:c2 :g2 :e2 :c2 :g2 nil nil :g2 :c3 :c3 :b2 :a2 :g2]
        [:g3 nil :f3 :d3 :e3 nil :c3 :d3 :e3 :f3 :d3 :g3 :e3]]
       rand-nth))

(defn melody []
  [:g4 nil nil :c4 nil nil :d#4 nil :g4 nil :c4 nil :g#4 nil :c4 nil])

(comment
  (schedule! my-notes (next-bar 120 4)))

(defn arp1 []
  (let [es [:c1 :g1 :a#1 :d2 :d#2]]
    (concat es (drop 1 (reverse (drop 1 es))))))

(defn bass2 []
  [nil :c0 nil :c0 nil nil nil :c1 nil nil nil :c0 nil :d#2 :c2 :c1])

(comment
  (loop! {:offset (next-bar 120 4)
          :tempo 120
          :loop-length 4
          :channel 6}
         #'bd2))


;;; fluidsynth
;;; fluidsynth -a pulseaudio -m alsa_seq -l /usr/share/soundfonts/freepats-general-midi.sf2

(defn init []
  (-> (get-output-p "E")
      (.then #(swap! !app-state assoc :output %))))



(defn stop-sequence! [id]
  (let [{:keys [callback-id]} (get-in @!app-state [:sequences id])]
    (js/clearTimeout callback-id)
    (swap! !app-state update :sequences dissoc id)))



(defn ^:export main []
  (init)
  (views/render-app {:!app-state !app-state
                     :stop-sequence stop-sequence!}))
