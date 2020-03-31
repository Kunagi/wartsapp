(ns wartsapp.patient
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(p/configure
 {:id-resolver (fn [event] (-> event :patient/id))})


(def-event :nummer-gezogen
  (fn [this event]
      (-> this
          (assoc :patient/nummer (-> event :nummer)))))

(def-event :eingecheckt
  (fn [this event]
    (-> this
        (assoc :patient/eingecheckt (-> event :event/time))
        (assoc :patient/status :eingecheckt))))
