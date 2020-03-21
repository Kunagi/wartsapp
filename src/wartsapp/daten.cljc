(ns wartsapp.daten)


(defn- new-uuid []
  #?(:clj (-> (java.util.UUID/randomUUID) .toString)
     :cljs (str (random-uuid))))


(defn- ts []
  #?(:clj (-> (System/currentTimeMillis))
     :cljs (js/Date.now)))


;;; warteschlange


(defn leere-schlange []
  {:id (new-uuid)
   :bezeichnung "Neue Warteschlange"
   :plaetze [] ;; vom typ ticket
   :checkin-fehler nil})



(defn ticket-eingecheckt? [schlange ticket-nummer]
  (->> schlange
       :plaetze
       (map :nummer)
       (filter (fn [nummer] (= nummer ticket-nummer)))
       first))


(defn finde-ticket-by-nummer [tickets ticket-nummer]
  (->> tickets
       (filter (fn [ticket] (= ticket-nummer (:nummer ticket))))
       first))


(defn finde-ticket-by-id [tickets ticket-id]
  (->> tickets
       (filter (fn [ticket] (= ticket-id (:id ticket))))
       first))

(defn checke-ein [schlange freie-tickets ticket-nummer]
  (let [schlange (dissoc schlange :checkin-fehler)
        ticket (finde-ticket-by-nummer (vals freie-tickets) ticket-nummer)
        bereits-eingecheckt? (ticket-eingecheckt? schlange ticket-nummer)
        ticket (if ticket
                 (assoc ticket :eingecheckt (ts)))]
    (cond
      bereits-eingecheckt? [schlange ticket]
      ticket [(update schlange :plaetze conj ticket) ticket]
      :else [(assoc schlange :checkin-fehler (str "Ticket-Nummer unbekannt")) nil])))


(defn update-ticket [schlange ticket-id props]
  (let [tickets (-> schlange :plaetze)
        tickets (map (fn [ticket]
                       (if-not (= ticket-id (:id ticket))
                         ticket
                         (merge ticket props)))
                     tickets)]
    (assoc schlange :plaetze tickets)))

;;; tickets

;; TODO https://github.com/zelark/nano-id

(defn neue-ticket-nummer []
  (.substring (new-uuid) 0 3))

(defn neues-ticket []
  {:nummer (neue-ticket-nummer)
   :id (new-uuid)
   :erstellt (ts)
   :praxis-patient ""})
