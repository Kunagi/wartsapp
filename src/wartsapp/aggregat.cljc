(ns wartsapp.aggregat
  (:require
   [clojure.spec.alpha :as s]

   [kcu.utils :as u]
   [kcu.aggregator :refer [def-command]]

   [wartsapp.schlange]
   [wartsapp.ticket]
   [wartsapp.freie-tickets]))


(def-command :eroeffne-schlange
  (fn [context {:keys [id]}]
    {:events {[:wartsapp.schlange id]
              [[:eroeffnet {:id id}]]}}))


(def-command :ziehe-ticket
  (fn [context {:keys [id]}]
    (let [nummer "abc"
          timestamp (context :timestamp)]
      {:events {
                [:wartsapp.freie-tickets]
                [[:gezogen {:nummer nummer
                            :id id}]]

                [:wartsapp.ticket id]
                [[:gezogen {:id id
                            :nummer nummer
                            :zeit timestamp}]]}

       :messages ["Sie haben jetzt ein Warteticket mit einer Nummer für die Praxis."]})))


(def-command :checke-ein
  (fn [context {:keys [schlange-id ticket-nummer]}]
    (let [freie-tickets (context :projection :wartsapp.freie-tickets)
          ticket-id (get-in freie-tickets [:nummer->ticket-id ticket-nummer])]
       (if-not ticket-id
         {:rejection (str "Ticket " ticket-nummer " nicht gefunden")}
         (let [ticket (context :projection :wartsapp.ticket ticket-id)
               ticket (dissoc ticket :projection/projector)
               time (context :timestamp)]
           {:events {
                     [:wartsapp.freie-tickets]
                     [[:entfernt {:nummer ticket-nummer}]]

                     [:wartsapp.schlange schlange-id]
                     [[:ticket-eingecheckt {:ticket ticket :time time}]]

                     [:wartsapp.ticket ticket-id]
                     [[:eingecheckt {:schlange schlange-id :time time}]]}})))))


(def-command :aendere-ticket
  (fn [context {:keys [id props]}]
    (let [ticket (context :projection :wartsapp.ticket id)
          schlange-id (get ticket :schlange)]
      {:events {
                [:wartsapp.ticket id]
                [[:geaendert {:props props}]]

                [:wartsapp.schlange schlange-id]
                [[:ticket-geaendert {:id id :props props}]]}})))
