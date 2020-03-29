(ns wartsapp.freie-tickets
  (:require
   [clojure.spec.alpha :as s]
   [kcu.utils :as u]
   [kcu.projector :as p :refer [def-event]]))


(def-event :gezogen
  (fn [this {:keys [nummer id]}]
    (assoc-in this [:nummer->ticket-id nummer] id)))


(def-event :entfernt
  (fn [this {:keys [nummer]}]
    (update this :nummer->ticket-id dissoc nummer)))
