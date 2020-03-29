(ns wartsapp.ticket
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(def-event :gezogen
  (fn [this {:keys [id nummer zeit]}]
    (assoc this
           :id id
           :nummer nummer
           :gezogen zeit
           :status :gezogen)))


(def-event :eingecheckt
  (fn [this {:keys [schlange zeit]}]
    (assoc this
           :eingecheckt zeit
           :schlange schlange
           :status :eingecheckt)))

(def-event :aufgerufen
  (fn [this {:keys [zeit]}]
    (throw (ex-info "boo" {}))
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
