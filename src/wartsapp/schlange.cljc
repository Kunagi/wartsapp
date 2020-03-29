(ns wartsapp.schlange
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(def-event :ticket-eingecheckt
  (fn [this {:keys [ticket zeit]}]
    (let [ticket (assoc ticket :eingecheckt zeit)]
      (-> this
         (assoc-in [:plaetze (-> ticket :id)] ticket)))))


(def-event :ticket-geaendert
  (fn [this {:keys [id props]}]
    (-> this
        (update-in [:plaetze id] #(merge % props)))))


(def-event :ticket-entfernt
  (fn [this {:keys [id]}]
    (-> this
        (update :plaetze dissoc id))))


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
