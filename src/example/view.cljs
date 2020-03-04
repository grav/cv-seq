(ns example.view
  (:require [reagent.core :as r]))

(defn block []
  (let [!lstate (r/atom nil)]
    (fn [{:keys [selected on-select playing]}]
      [:div {:style {:width 50
                     :height 50
                     :border-style :solid
                     :border-widh 1
                     :pointer :cursor
                     :background-color (if @!lstate
                                         (if selected
                                           :lightblue
                                           "#6666ff")
                                         (if selected
                                           (if playing
                                             "#aaff00"
                                             :green)
                                           (if playing :yellow
                                                       :white)))}
             :on-mouse-down on-select
             :on-mouse-over #(reset! !lstate true)
             :on-mouse-out #(reset! !lstate false)}])))

(def n 10)

(defn sequence-view [{:keys [beat steps on-steps-changed]}]
  [:div {:style {:display :flex}}
   (for [[i s] (map vector (range) steps)]
     ^{:key i}
     [:div
      (for [[j selected] (->> (concat (repeat (inc (* s n)) true)
                                      (repeat false))
                              (take n)
                              (map vector (range))
                              reverse)]

        ^{:key j}
        [block {:selected selected
                :playing (= i (mod beat (count steps)))
                :on-select #(on-steps-changed (assoc steps i (/ j 10)))}])])])





