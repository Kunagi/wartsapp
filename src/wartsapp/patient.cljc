(ns wartsapp.patient
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(p/configure
 {:id-resolver (fn [event] (-> event :patient/id))})


(def-event :ticket-gezogen
  (fn [this event]
      (-> this
          (update :events conj event))))
