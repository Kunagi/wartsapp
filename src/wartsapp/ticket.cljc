(ns wartsapp.ticket
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


#_(macroexpand-1 '(def-attr nummer))

(def-event :gezogen
  (fn [this event]
    (assoc this
           :ticket/id (-> event :ticket/id)
           :ticet/nummer (-> event :ticet/nummer)
           :ticket/gezogen (-> event :event/time)
           :ticket/status :gezogen)))


(def-event :eingecheckt
  (fn [this {:keys [schlange zeit]}]
    (assoc this
           :eingecheckt zeit
           :schlange schlange
           :status :eingecheckt)))

(def-event :aufgerufen
  (fn [this {:keys [zeit]}]
    (assoc this
           :aufgerufen zeit
           :status :aufgerufen)))

(def-event :unterwegst
  (fn [this {:keys [zeit]}]
    (assoc this
           :unterwegs zeit
           :status :unterwegs)))

(def-event :entfernt
  (fn [this args]
    (assoc this :enternt? true)))
