(ns example.core
  (:require [reagent.core :as r]))

(defonce !app-state (r/atom nil))

(defn app []
  (let [{:keys [message]} @!app-state]
    [:p (or message "hello")]))


(defn ^:dev/after-load main []
  (r/render [app] (js/document.getElementById "app")))

