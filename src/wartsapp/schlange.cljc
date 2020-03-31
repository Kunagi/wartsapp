(ns wartsapp.schlange
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(p/configure
 {:id-resolver (fn [event] (-> event :schlange/id))
  :type :Schlange})


(def-event :eingecheckt
  (fn [this event]
    (let [patient-id (u/getm event :patient/id)
          patient {:patient/id patient-id
                   :patient/nummer (u/getm event :nummer)
                   :patient/eingecheckt (-> event :event/time)}]
      (-> this
          (assoc-in [:plaetze patient-id] patient)))))


(def-event :aufgerufen
  (fn [this event]
    (let [patient-id (u/getm event :patient/id)]
      (-> this
          (assoc-in [:plaetze patient-id :patient/aufgerufen]
                    (-> event :event/time))))))


(def-event :aufruf-bestaetigt
  (fn [this event]
    (let [patient-id (u/getm event :patient/id)]
      (-> this
          (assoc-in [:plaetze patient-id :patient/aufruf-bestaetigt]
                    (-> event :event/time))))))


(def-event :von-schlange-entfernt
  (fn [this event]
    (let [patient-id (u/getm event :patient/id)]
      (-> this
          (assoc-in [:plaetze patient-id :patient/entfernt]
                    (-> event :event/time))))))
