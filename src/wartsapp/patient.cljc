(ns wartsapp.patient
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(def-event :ticket-gezogen
  (fn [this {:keys [ticket-id ticket-nummer zeit patient-id]}]
    (if (not= patient-id (-> this :projection/entity-id))
      this
      (-> this
          (assoc :ticket {:id ticket-id
                          :nummer ticket-nummer
                          :gezogen zeit
                          :status :gezogen})))))
