(ns wartsapp.daten)


(defn- new-uuid []
  #?(:clj (-> (java.util.UUID/randomUUID) .toString)
     :cljs (str (random-uuid))))


(defn- ts []
  #?(:clj (-> (System/currentTimeMillis))
     :cljs (js/Date.now)))


(defn- update-etag
  [entity]
  (when entity
    (assoc entity :etag (new-uuid))))


(defn- updatev-etags
  [entities]
  (mapv update-etag entities))


;;; warteschlange


(defn leere-schlange [id]
  {:id id
   :etag id
   :plaetze [] ;; vom typ ticket
   :checkin-fehler nil})


(defn neue-schlange-id []
  (new-uuid))


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


(defn checke-ein-in-schlange [schlange freie-tickets ticket-nummer]
  (let [schlange (dissoc schlange :checkin-fehler)
        ticket (finde-ticket-by-nummer (vals freie-tickets) ticket-nummer)
        bereits-eingecheckt? (ticket-eingecheckt? schlange ticket-nummer)
        ticket (when ticket
                 (assoc ticket :eingecheckt (ts)))]
    (updatev-etags
     (cond
       bereits-eingecheckt? [schlange ticket]
       ticket [(update schlange :plaetze conj ticket) ticket]
       :else [(assoc schlange :checkin-fehler (str "Ticket-Nummer unbekannt")) nil]))))


(defn update-ticket [schlange ticket-id props]
  (let [tickets (-> schlange :plaetze)
        tickets (map (fn [ticket]
                       (if-not (= ticket-id (:id ticket))
                         ticket
                         (-> ticket
                             (merge props)
                             update-etag)))
                     tickets)]
    (-> schlange
        (assoc :plaetze tickets)
        update-etag)))



;;; tickets

(defonce !ticket-nummern (atom #{}))

(defn neue-ticket-nummer [length]
  (let [nummer (.substring (new-uuid) 0 length)]
    (if (contains? @!ticket-nummern nummer)
      (neue-ticket-nummer (inc length))
      (do
        (swap! !ticket-nummern conj nummer)
        nummer))))

(defn neue-ticket-id []
  (new-uuid))

(defn neues-ticket [ticket-id]
  {:nummer (neue-ticket-nummer 3)
   :id ticket-id
   :etag ticket-id
   :erstellt (ts)
   :praxis-patient ""})


;; app state

(defn neues-system []
  {:freie-tickets {}
   :schlangen {}
   :ticket-id->schlange-id {}})


(defn ziehe-ticket [system neue-ticket-id]
  (let [ticket (neues-ticket neue-ticket-id)]
    (-> system
        (assoc-in [:freie-tickets neue-ticket-id] ticket))))


(defn freies-ticket [system ticket-id]
  (get-in system [:freie-tickets ticket-id]))


(defn eroeffne-schlange [system schlange-id]
  (let [schlange (leere-schlange schlange-id)]
    (-> system
        (assoc-in [:schlangen schlange-id] schlange))))


(defn checke-ein [system schlange-id ticket-nummer]
  (let [schlange (get-in system [:schlangen schlange-id])]
    (if-not schlange
      system
      (let [freie-tickets (get system :freie-tickets)
            [schlange ticket] (checke-ein-in-schlange schlange freie-tickets ticket-nummer)
            ticket-id (-> ticket :id)]
        (-> system
            (assoc-in [:schlangen schlange-id] schlange)
            (update :freie-tickets dissoc ticket-id)
            (assoc-in [:ticket-id->schlange-id ticket-id] schlange-id))))))


(defn update-ticket-by-praxis [system ticket-id props]
  (let [schlange-id (get-in system [:ticket-id->schlange-id ticket-id])
        schlange (get-in system [:schlangen schlange-id])
        schlange (update-ticket schlange ticket-id props)]
    (-> system
        (assoc-in [:schlangen schlange-id] schlange))))


(defn update-ticket-by-patient [system ticket-id props]
  (let [schlange-id (get-in system [:ticket-id->schlange-id ticket-id])
        schlange (get-in system [:schlangen schlange-id])
        schlange (update-ticket schlange ticket-id props)]
    (-> system
        (assoc-in [:schlangen schlange-id] schlange))))


(defn ticket-fuer-patient [system ticket-id]
  (let [ticket (get-in system [:freie-tickets ticket-id])]
    (if ticket
      (-> ticket
          (assoc :eingecheckt? false))
      (let [schlange-id (get-in system [:ticket-id->schlange-id ticket-id])
            schlange (get-in system [:schlangen schlange-id])
            ticket (finde-ticket-by-id (-> schlange :plaetze) ticket-id)]
        (-> ticket
            (assoc :eingecheckt? true))))))


(defn schlange-fuer-praxis [system schlange-id]
  (let [schlange (get-in system [:schlangen schlange-id])]
    schlange))
