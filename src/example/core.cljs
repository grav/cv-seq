(ns example.core
  (:require [reagent.core :as r]
            [idb]))

(defonce !app-state (r/atom nil))

(defn app []
  (let [{:keys [message]} @!app-state]
    [:p (or message "hello")]))

; (defn upgrade-db [db]
;   (println 'upgrade-db)
;   (.createObjectStore db "my-store2"
;                       #js {"keyPath" "id"}))

; (defn put [m]
;   (-> (.open idb "my-db" 1 upgrade-db)
;       (.then (fn [db]
;                (let [tx (.transaction db "my-store2" "readwrite")]
;                  (-> tx (.objectStore "my-store2") (.put (clj->js m)))
;                  (.-complete tx))))))

(defn ^:dev/after-load main []
  (r/render [app] (js/document.getElementById "app")))

