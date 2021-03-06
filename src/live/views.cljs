(ns live.views
  (:require [reagent.core :as r]
            [live.core]))

(defn sequence-view [id {{:keys [function channel]} :sequence
                         :keys [on-click]}]
  [:div
   {:style {:margin 2
            :cursor :pointer
            :background :green}
    :on-click (fn []
                (on-click id))}
   (str function " #" channel "")])

(defn app [{:keys [!app-state stop-sequence]}]
  [:div
   (if (:output @!app-state)
     [:div (str "Device: " (pr-str (.-name (:output @!app-state))))]
     [:div "no device ..."])
   [:div {:style {:color :green}} "Sequences"]
   [:div
    (for [[id sequence] (:sequences @!app-state)]
      ^{:key id} [sequence-view id {:sequence sequence
                                    :on-click stop-sequence}])]])

(defn ^:dev/after-load main []
  (-> (live.core/init)
      (.then #(r/render [app {:!app-state live.core/!app-state
                              :stop-sequence live.core/stop-sequence!}]
                        (js/document.getElementById "app")))))
