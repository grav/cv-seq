(ns example.core
  (:require [reagent.core :as r]
            [example.view :as v]
            [example.seq :as seq]
            [clojure.pprint]))

(def n-steps 8)

(defonce !app-state (r/atom {:steps (vec (repeat n-steps 0))
                             :ctx nil}))

(defn setup-ctx! []
  (let [{:keys [ctx]} @!app-state]
    (when ctx
      (.close ctx)))

  (let [{:keys [ctx]} (swap! !app-state assoc :ctx (js/window.AudioContext.))
        osc (.createOscillator ctx) #_(.createConstantSource ctx)
        _ (set! (.-type osc) "sine")
        _ (.setValueAtTime (.-frequency osc) 8(.-currentTime ctx))
        gain (.createGain ctx)
        _ (.setValueAtTime (.-gain gain) 0.5 (.-currentTime ctx))

        scriptNode (.createScriptProcessor ctx 4096 1 1)
        _ (set! (.-onaudioprocess scriptNode) (js/algebra #(+ 0.5 %)))]

    (def the-gain gain)
    (.connect osc gain)
    (.connect gain scriptNode)
    (.connect scriptNode (.-destination ctx))
    (.start osc)))

(defn app []
  [:div
   (let [{:keys [playing steps ctx]} @!app-state]
     (println 'ctx ctx)
     (if ctx
       [:div
        [:button {:on-click (fn []
                              (swap! !app-state assoc :playing true)
                              (seq/play-repeatedly
                                {:now-fn (fn [] (.now (.-performance js/window)))
                                 :ding (fn [[_ _ v t]]
                                         (println (.-currentTime ctx))
                                         _ (.setValueAtTime (.-gain the-gain) 0.5 t)
                                         (print v t))
                                 :on-update-beat (fn [%]
                                                   (swap! !app-state assoc :beat %))
                                 :seq-transform (fn [seq]
                                                  [{:device nil
                                                    :sequence {:sequence (for [s seq]
                                                                           [{:note s}])}}])
                                 :!sequences (r/cursor !app-state [:steps])}
                                {:beat 0
                                 :time 0}))
                  :disabled playing} "Start"]
        [v/sequence-view {:steps steps
                          :step-playing 2
                          :on-steps-changed #(swap! !app-state assoc :steps %)}]]
       [:button {:on-click (fn []
                             (setup-ctx!))}
        "Setup Audio"]))
   [:div [:pre (with-out-str (clojure.pprint/pprint @!app-state))]]])



(defn ^:dev/after-load main []
  (let [{:keys [ctx]} @!app-state]
    (when ctx
      (setup-ctx!)))
  (r/render [app] (js/document.getElementById "app")))







