(ns wartsapp.manager
  (:require
   [clojure.spec.alpha :as s]

   [kcu.utils :as u]
   [kcu.aggregator :refer [def-command def-event]]

   [wartsapp.patient]
   [wartsapp.schlange]
   [wartsapp.freie-tickets]))


;; damit jedes system (für testzwecke) zuerst immer folgende nummern generiert
(def vordefinierte-nummern ["a1" "a2" "a3" "a4" "a5"])

(defn- vordefinierte-nummer [verbrauchte-nummern idx]
  (when (< idx (count vordefinierte-nummern))
    (let [nummer (get vordefinierte-nummern idx)]
      (if (contains? verbrauchte-nummern nummer)
        (vordefinierte-nummer verbrauchte-nummern (inc idx))
        nummer))))

(defn random-nummer [length]
  (.substring (u/random-uuid-string) 0 length))


(defn- naechste-freie-nummer [verbrauchte-nummern length]
  (let [neue-nummer (or (vordefinierte-nummer verbrauchte-nummern 0)
                        (random-nummer length))]
    (if (contains? verbrauchte-nummern neue-nummer)
      (naechste-freie-nummer verbrauchte-nummern (inc length))
      neue-nummer)))


(def-command :ziehe-ticket
  (fn [this args context]
    (let [nummer (naechste-freie-nummer (-> this :nummern :verbrauchte) 3)
          id (context :random-uuid)]
      {:event/name :ticket-gezogen
       :ticket/id id
       :ticket/nummer nummer
       :patient/id (-> args :patient/id)})))


(def-event :ticket-gezogen
  {:scope [:nummern]}
  (fn [nummern event]
    (let [nummer (-> event :ticket/nummer)]
      (-> nummern

          ;; ticket-id zur nummer merken
          (assoc-in [:tickets nummer]
                    (-> event :ticket/id))

          ;; nummer verbrauchen
          (update :verbrauchte #(if %
                                  (conj % nummer)
                                  #{nummer}))))))


(def-command :checke-ein
  (fn [this args context]
    (let [nummer (-> args :ticket/nummer)
          ticket-id (-> this :nummern :tickets (get nummer))]
       (if-not ticket-id
         [{:rejection/text (str "Ticket " nummer " nicht gefunden")}]
         [{:event/name :ticket-eingecheckt
           :ticket/id ticket-id
           :ticket/nummer nummer
           :schlange/id (-> args :schlange/id)}]))))


(def-event :ticket-eingecheckt
  {:scope [:nummern]}
  (fn [nummern event]
    (let [nummer (-> event :ticket/nummer)]
      (update nummern :tickets dissoc nummer))))


(def-command :rufe-auf
  (fn [this args context]
    [{:event/name :ticket-aufgerufen
      :ticket/id (-> args :ticket/id)}]))


(def-command :bestaetige-aufruf
  (fn [this args context]
     [{:event/name :ticket-aufruf-bestaetigt
       :ticket/id (-> args :ticket-/id)}]))


(def-command :entferne-ticket
  (fn [this args context]
    {:event/name :ticket-entfernt
     :ticket/id (-> args :ticket-/id)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
