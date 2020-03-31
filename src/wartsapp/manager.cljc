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


(def-command :ziehe-nummer
  (fn [this args context]
    (let [nummer (naechste-freie-nummer (-> this :nummern :verbrauchte) 3)
          id (context :random-uuid)]
      {:event/name :nummer-gezogen
       :nummer nummer
       :patient/id (-> args :patient/id)})))


(def-event :nummer-gezogen
  {:scope [:nummern]}
  (fn [nummern event]
    (let [nummer (-> event :nummer)]
      (-> nummern

          ;; ticket-id zur nummer merken
          (assoc-in [:patienten nummer]
                    (-> event :patient/id))

          ;; nummer verbrauchen
          (update :verbrauchte #(if %
                                  (conj % nummer)
                                  #{nummer}))))))


(def-command :checke-ein
  (fn [this args context]
    (let [nummer (-> args :nummer)
          patient-id (-> this :nummern :patienten (get nummer))]
       (if-not patient-id
         [{:rejection/text (str "Nummer " nummer " nicht gefunden")}]
         [{:event/name :eingecheckt
           :patient/id patient-id
           :nummer nummer
           :schlange/id (-> args :schlange/id)}]))))


(def-event :eingecheckt
  {:scope [:nummern]}
  (fn [nummern event]
    (let [nummer (-> event :patient/nummer)]
      (update nummern :patienten dissoc nummer))))


(def-command :rufe-auf
  (fn [this args context]
    [{:event/name :aufgerufen
      :patient/id (-> args :patient/id)}]))


(def-command :bestaetige-aufruf
  (fn [this args context]
     [{:event/name :aufruf-bestaetigt
       :patient/id (-> args :patient-/id)}]))


(def-command :entferne-patient-von-schlange
  (fn [this args context]
    {:event/name :von-schlange-entfernt
     :patient/id (-> args :patient/id)
     :schlange/id (-> args :schlange/id)}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
