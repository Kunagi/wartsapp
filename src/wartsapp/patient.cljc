(ns wartsapp.patient
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(p/configure
 {:id-resolver (fn [event] (-> event :patient/id))
  :type :Patient})


(def-event :nummer-gezogen
  (fn [this event]
    (-> this
        (assoc :patient/nummer (-> event :nummer))
        (assoc :patient/status :frei))))


(def-event :eingecheckt
  (fn [this event]
    (-> this
        (assoc :patient/eingecheckt (-> event :event/time))
        (assoc :patient/status :eingecheckt))))


(def-event :aufgerufen
  (fn [this event]
    (assoc this
           :patient/aufgerufen (-> event :event/time)
           :patient/status :aufgerufen)))


(def-event :aufruf-bestaetigt
  (fn [this event]
    (assoc this
           :patient/aufruf-bestaetigt (-> event :event/time)
           :patient/status :aufruf-bestaetigt)))


(def-event :von-schlange-entfernt
  (fn [this event]
    (assoc this :patient/enternt (-> event :event/time)
                :patient/status :entfernt)))
