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
           :gezogen zeit)))


(def-event :eingecheckt
  (fn [this {:keys [schlange time]}]
    (assoc this
           :eingecheckt time
           :schlange schlange)))


(def-event :geaendert
  (fn [this {:keys [props]}]
    (merge this props)))


