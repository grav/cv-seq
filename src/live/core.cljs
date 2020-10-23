(ns live.core
  (:require [clojure.string :as str]
            [cljs-bean.core :refer [bean]]
            [reagent.core :as r]
            [clojure.pprint]))

(def output-name-prefix "Synth input port")

(def note-on 0x90)

(def note-off 0x80)

(def vol 0x7f)

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


(defn note->midi-message [{:keys [note
                                  length
                                   velocity
                                   offset
                                   tempo
                                  sustain
                                  transpose
                                  channel]}]
  (let [secs-per-beat (bpm->secs-per-beat tempo)
        note-val (note->data1 note)]
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
      :channel (or channel 1)}]))

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

(defn play-notes! [{:keys [offset output] :as args} notes]
  (doseq [{:keys [type data1 data2 time channel]} (sequence->notes
                                                   args
                                                   notes)]
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
                                                            (notes-fn-var))
                                               (catch js/Error e
                                                   (println 'boom e)))
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

(defn arp1 []
  (let [es [:c1 :g1 :a#1 :d2 :d#2]]
    (concat es (drop 1 (reverse (drop 1 es))))))

(defn bass2 []
  [nil :c0 nil :c0 nil nil nil :c1 nil nil nil :c0 nil :d#2 :c2 :c1])


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
