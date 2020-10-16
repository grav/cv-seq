(ns live.views
  (:require [reagent.core :as r]))

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
 [:div "Sequences"
  [:div
   (for [[id sequence] (:sequences @!app-state)]
     ^{:key id} [sequence-view id {:sequence sequence
                                   :on-click stop-sequence}])]])

(defn render-app [{:keys [!app-state stop-sequence]}]
  (r/render [app {:!app-state !app-state
                  :stop-sequence stop-sequence}] (js/document.getElementById "app")))
