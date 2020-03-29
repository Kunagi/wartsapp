(ns wartsapp.schlange
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(s/def ::id string?)


(def-event :eroeffnet
  (fn [this {:keys [id]}]
    this
    (-> this
        (assoc :id id
               :plaetze {}))))


(def-event :ticket-eingecheckt
  (fn [this {:keys [ticket time]}]
    (let [ticket (assoc ticket :eingecheckt time)]
      (-> this
         (assoc-in [:plaetze (-> ticket :id)] ticket)))))


(def-event :ticket-geaendert
  (fn [this {:keys [id props]}]
    (-> this
        (update-in [:plaetze id] #(merge % props)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


#_(-> :kcu.schlange
    p/projector
    (p/project
     [[:eroeffnet {:id "s1"
                   :time 1}]
      [:ticket-eingecheckt {:ticket {:id "t1"}
                            :time 1}]
      [:ticket-geaendert {:ticket-id "t1"
                          :props {:unterwegs 123}}]]))
