(ns wartsapp.main
  (:require
   [clojure.edn :as edn]

   [kunagi-base.logging.tap-formated]
   [kunagi-base.enable-asserts]
   [kunagi-base.appconfig.load-as-server]

   [kunagi-base.appconfig.api :as appconfig]
   [kunagi-base.modules.startup.api :as startup]
   [kunagi-base.appmodel :refer [def-module]]

   ;; load kunagi-base modules
   [kunagi-base-server.modules.http-server.model :refer [def-route]]
   [kunagi-base-server.modules.browserapp.model]

   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.daten :as daten]))


(def state-path "app-data/state.edn")

(defn read-state-from-disk! []
  (let [file (-> state-path java.io.File.)]
    (when (-> file .exists)
      (-> file slurp edn/read-string))))

(defn write-state-to-disk! [state]
  (let [file (-> state-path java.io.File.)]
    (when-not (-> file .exists) (-> file .getParentFile .mkdirs))
    (spit state-path state)))


(defn on-agent-error [_agent ex]
  (tap> [:err ::on-db-agent-error ex]))


(defonce db (agent {}))
(defonce !state (agent (or
                        (read-state-from-disk!)
                        {:freie-tickets {}
                         :schlangen {}
                         :ticket-id->schlange-id {}})
                       :error-mode :continue
                       :error-handler on-agent-error))

(defn transact [f]
  (send-off !state
            (fn [state]
              (let [state (f state)]
                (write-state-to-disk! state)
                state))))


(defn serve-ziehe-ticket [context]
  (let [ticket (daten/neues-ticket)
        ticket-id (-> ticket :id)]
    (transact
      (fn [state]
        (assoc-in state [:freie-tickets ticket-id] ticket)))
    (tap> [:inf ::ticket-erstellt ticket])
    (str ticket)))


(defn serve-eroeffne-schlange [context]
  (let [schlange (daten/leere-schlange)
        schlange-id (-> schlange :id)]
    (transact
      (fn [state]
        (assoc-in state [:schlangen schlange-id] schlange)))
    (tap> [:inf ::schlange-eroeffnet schlange])
    (str schlange)))


(defn serve-checke-ein [context]
  (let [params (-> context :http/request :params)
        schlange-id (-> params :schlange)
        ticket-nummer (-> params :ticket)
        schlange (get-in @!state [:schlangen schlange-id])]
    (if-not schlange
      {:status 416
       :body (str "Schlange nicht gefunden: " schlange-id)}
      (let [freie-tickets (get @!state :freie-tickets)
            [schlange ticket] (daten/checke-ein schlange freie-tickets ticket-nummer)
            ticket-id (-> ticket :id)]
        (transact
          (fn [state]
            (-> state
                (assoc-in [:schlangen schlange-id] schlange)
                (update :freie-tickets dissoc ticket-id)
                (assoc-in [:ticket-id->schlange-id ticket-id] schlange-id))))
        (str schlange)))))


(defn serve-ticket [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :id)
        ticket (get-in @!state [:freie-tickets ticket-id])]
    (if ticket
      (-> ticket
          (assoc :eingecheckt? false)
          str)
      (let [schlange-id (get-in @!state [:ticket-id->schlange-id ticket-id])
            schlange (get-in @!state [:schlangen schlange-id])
            ticket (daten/finde-ticket-by-id (-> schlange :plaetze) ticket-id)]
        (-> ticket
            (assoc :eingecheckt? true)
            str)))))

(defn serve-schlange [context]
  (let [params (-> context :http/request :params)
        schlange-id (-> params :id)
        schlange (get-in @!state [:schlangen schlange-id])]
    (str schlange)))

(defn serve-update-ticket-by-praxis [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))
        schlange-id (get-in @!state [:ticket-id->schlange-id ticket-id])
        schlange (get-in @!state [:schlangen schlange-id])
        schlange (daten/update-ticket schlange ticket-id props)]
    (tap> [:inf ::update {:ticket-id ticket-id :props props}])
    (transact
      (fn [state]
        (assoc-in state [:schlangen schlange-id] schlange)))
    (str schlange)))

(defn serve-update-ticket-by-patient [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))
        schlange-id (get-in @!state [:ticket-id->schlange-id ticket-id])
        schlange (get-in @!state [:schlangen schlange-id])
        schlange (daten/update-ticket schlange ticket-id props)
        ticket (daten/finde-ticket-by-id (-> schlange :plaetze) ticket-id)]
    (tap> [:inf ::update {:ticket-id ticket-id :props props}])
    (transact
      (fn [state]
        (assoc-in state [:schlangen schlange-id] schlange)))
    (str ticket)))

(defn serve-state [context]
  (str @!state))

(def-module
  {:module/id ::demo-serverapp})

(def-route
  {:route/id ::api-state
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/state"
   :route/serve-f #(serve-state %)
   :route/req-perms []})

(def-route
  {:route/id ::api-update-ticket-by-praxis
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/update-ticket-by-praxis"
   :route/serve-f #(serve-update-ticket-by-praxis %)
   :route/req-perms []})

(def-route
  {:route/id ::api-update-ticket-by-patient
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/update-ticket-by-patient"
   :route/serve-f #(serve-update-ticket-by-patient %)
   :route/req-perms []})

(def-route
  {:route/id ::api-ticket
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/ticket"
   :route/serve-f #(serve-ticket %)
   :route/req-perms []})

(def-route
  {:route/id ::api-ziehe-ticket
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/ziehe-ticket"
   :route/serve-f #(serve-ziehe-ticket %)
   :route/req-perms []})

(def-route
  {:route/id ::api-schlange
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/schlange"
   :route/serve-f #(serve-schlange %)
   :route/req-perms []})

(def-route
  {:route/id ::api-eroeffne-schlange
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/eroeffne-schlange"
   :route/serve-f #(serve-eroeffne-schlange %)
   :route/req-perms []})

(def-route
  {:route/id ::api-checke-ein
   :route/module [:module/ident :demo-serverapp]
   :route/path "/api/checke-ein"
   :route/serve-f #(serve-checke-ein %)
   :route/req-perms []})

(appconfig/set-default-config!
 {:http-server/oauth {:google {:enabled? false}}})


(defn -main []
  (startup/start! {:app/info appinfo}))
