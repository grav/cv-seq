(ns example.core
  (:require [reagent.core :as r]
            [example.view :as v]
            [example.seq :as seq]))

(def n-steps 10)

(defonce !steps (r/atom (vec (repeat n-steps 0))))

(defonce !ctx (atom nil))

(defonce !app-state (r/atom nil))

(defn  setup! []
  (println "setup")
  (when-let [ctx @!ctx]
    (.close ctx))

  (let [ctx (reset! !ctx (js/window.AudioContext.))
        osc (.createOscillator ctx)
        _ (set! (.-type osc) "sine")
        _ (.setValueAtTime (.-frequency osc) 4 (.-currentTime ctx))
        gain (.createGain ctx)
        _ (.setValueAtTime (.-gain gain) 0.5 (.-currentTime ctx))

        scriptNode (.createScriptProcessor ctx 4096 1 1)
        _ (set! (.-onaudioprocess scriptNode) (js/algebra #(+ 0.5 %)))]

    (.connect osc gain)
    (.connect gain scriptNode)
    (.connect scriptNode (.-destination ctx))
    (.start osc)))

(defn app []
  [:div
   (if-let [_ctx @!ctx]
     (do
       (setup!)
       [:div
        [:button {:on-click #(seq/play-repeatedly
                               {:now-fn (fn [] (.now (.-performance js/window)))
                                :on-update-beat (fn [%]
                                                  (swap! !app-state assoc :beat %))
                                :seq-transform (fn [seq]
                                                 [{:device nil
                                                   :sequence {:sequence (for [s seq]
                                                                          [{:note s}])}}])
                                :!sequences !steps}
                               {:beat 0
                                :time 0})} "Start"]
        [v/sequence-view {:steps @!steps
                          :step-playing 2
                          :on-steps-changed #(reset! !steps %)}]])
     [:button {:on-click (fn []
                           (setup!))}
      "Start"])])



(defn ^:dev/after-load main []

  (r/render [app] (js/document.getElementById "app")))







