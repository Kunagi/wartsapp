(ns wartsapp.manager
  (:require
   [clojure.spec.alpha :as s]

   [kcu.utils :as u]
   [kcu.aggregator :refer [def-command]]

   [wartsapp.schlange]
   [wartsapp.ticket]
   [wartsapp.freie-tickets]))


(def-command :ziehe-ticket
  (fn [context {:keys [patient-id]}]
    (let [nummer "abc"
          id (context :random-uuid)
          zeit (context :timestamp)]
      {
       :devents [{:event/name :ticket-gezogen
                  :ticket/id id
                  :ticket/nummer nummer
                  :ticket/gezogen zeit
                  :patient/id patient-id}]

       :events {
                [:wartsapp.freie-tickets]
                [[:gezogen {:nummer nummer
                            :id id}]]

                [:wartsapp.ticket id]
                [[:gezogen {:id id
                            :nummer nummer
                            :zeit zeit}]]}

       :messages ["Sie haben jetzt ein Warteticket mit einer Nummer für die Praxis."]})))


(def-command :checke-ein
  (fn [context {:keys [schlange-id ticket-nummer]}]
    (let [freie-tickets (context :projection :wartsapp.freie-tickets)
          ticket-id (get-in freie-tickets [:nummer->ticket-id ticket-nummer])]
       (if-not ticket-id
         {:rejection (str "Ticket " ticket-nummer " nicht gefunden")}
         (let [ticket (context :projection :wartsapp.ticket ticket-id)
               ticket (dissoc ticket :projection/projector)
               zeit (context :timestamp)]

           {
            :devents [{:event/name :ticket-eingecheckt
                       :ticket/id ticket-id
                       :schlange/id schlange-id
                       :ticket/eingecheckt zeit}]

            :events {
                     [:wartsapp.freie-tickets]
                     [[:entfernt {:nummer ticket-nummer}]]

                     [:wartsapp.schlange schlange-id]
                     [[:ticket-eingecheckt {:ticket ticket :zeit zeit}]]

                     [:wartsapp.ticket ticket-id]
                     [[:eingecheckt {:schlange schlange-id :zeit zeit}]]}})))))


(def-command :rufe-auf
  (fn [context {:keys [ticket-id]}]
    (let [ticket (context :projection :wartsapp.ticket ticket-id)
          schlange-id (get ticket :schlange)
          zeit (context :timestamp)]
      {:browser-push-notifications [{:title "Sie wurden aufgerufen"
                                     :text "Machen Sie sich auf den Weg zur Praxis."
                                     :push-url "http://google-push....."
                                     :push-key "abcdefgh"}]
       :devents [ {:event/name :ticket-aufgerufen
                   :ticket/id ticket-id
                   :ticket/aufgerufen zeit}]
       :events {
                [:wartsapp.ticket ticket-id]
                [[:aufgerufen {:zeit zeit}]]

                [:wartsapp.schlange schlange-id]
                [[:ticket-geaendert {:id ticket-id :props {:aufgerufen zeit}}]]}})))


(def-command :bestaetige-aufruf
  (fn [context {:keys [ticket-id]}]
    (let [ticket (context :projection :wartsapp.ticket ticket-id)
          schlange-id (get ticket :schlange)
          zeit (context :timestamp)]

      {:devents [{:event/name :ticket-aufruf-bestaetigt
                  :ticket/id ticket-id
                  :ticket/aufruf-bestaetigt zeit}]

       :events {

                [:wartsapp.ticket ticket-id]
                [[:unterwegst {:zeit zeit}]]

                [:wartsapp.schlange schlange-id]
                [[:ticket-geaendert {:id ticket-id :props {:unterwegs zeit}}]]}})))


(def-command :entferne-ticket
  (fn [context {:keys [schlange-id ticket-id]}]
    (let [zeit (context :timestamp)]
      {:devents [{:event/name :ticket-entfernt
                  :ticket/id ticket-id
                  :ticket/entfernt zeit}]
       :events {
                [:wartsapp.schlange schlange-id]
                [[:ticket-entfernt {:id ticket-id}]]

                [:wartsapp.ticket ticket-id]
                [[:entfernt {}]]}})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn out [& args])

(def-command :unterwegse2
  (fn [context {:keys [ticket-id]}]
    (let [ticket (context :projection :wartsapp.ticket ticket-id)
          schlange-id (get ticket :schlange)
          time (context :timestamp)]
      (out :event [:wartsapp.ticket ticket-id]
                  [:unterwegst {:zeit time}])
      (out :event [:wartsapp.schlange schlange-id]
                  [{:id ticket-id :props {:unterwegs time}}]))))
