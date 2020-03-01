(ns example.view)

(defn block [{:keys [selected on-select playing]}]
  [:div {:style {:width 50
                 :height 50
                 :border-style :solid
                 :border-widh 1
                 :pointer :cursor
                 :background-color (if selected
                                     (if playing
                                       "#aaff00"
                                       :green)
                                     (if playing :yellow
                                                 :white))}
         :on-mouse-down on-select}])

(def n 10)

(defn sequence-view [{:keys [steps on-steps-changed step-playing]}]
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
                :playing (= i step-playing)
                :on-select #(on-steps-changed (assoc steps i (/ j 10)))}])])])





