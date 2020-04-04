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

  (let [ctx (js/window.AudioContext.)
        osc (.createOscillator ctx) #_(.createConstantSource ctx)
        _ (set! (.-type osc) "sine")
        _ (.setValueAtTime (.-frequency osc) 8 (.-currentTime ctx))
        gain (.createGain ctx)
        _ (.setValueAtTime (.-gain gain) 0.5 (.-currentTime ctx))

        scriptNode (.createScriptProcessor ctx 4096 1 1)
        _ (set! (.-onaudioprocess scriptNode) (js/algebra #(+ 0.5 %)))]

    (.connect osc gain)
    (.connect gain scriptNode)
    (.connect scriptNode (.-destination ctx))
    (.start osc)
    (swap! !app-state assoc :ctx ctx :osc osc :gain gain)))

(def ding
  (fn [[_ _ v t]]
    (let [{:keys [ctx osc gain]} @!app-state
          #_#_n (* 10 v)]
      (.setValueAtTime (.-frequency osc)
                       (-> (js/Math.pow 2 (/ v 12))
                           (* 330))
                       t)
      (println 'current-time (.-currentTime ctx) 't t)
      (.setValueAtTime (.-gain gain) 0.0 t)
      (.linearRampToValueAtTime (.-gain gain) 0.5 (+ t 0.01))
      (.linearRampToValueAtTime (.-gain gain) 0.0 (+ t 0.1))
      #_(.setValueAtTime (.-gain))
      #_(print v t))))

(defn app []
  [:div
   (let [{:keys [playing steps ctx]} @!app-state]
     (if ctx
       [:div
        [:button {:on-click (fn []
                              (swap! !app-state assoc :playing true)
                              (seq/play-repeatedly
                                {:now-fn (fn [] (.now (.-performance js/window)))
                                 :ding #'ding
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
                          :step-playing nil
                          :on-steps-changed #(swap! !app-state assoc :steps %)}]]
       [:button {:on-click (fn []
                             (setup-ctx!))}
        "Setup Audio"]))
   [:div [:pre (with-out-str (clojure.pprint/pprint @!app-state))]]])



(defn ^:dev/after-load main []
  (let [{:keys [ctx]} @!app-state]
    #_(when ctx
        (setup-ctx!)))
  (r/render [app] (js/document.getElementById "app")))







