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

   [kcu.files :as files]
   [kcu.txa :as txa]

   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.daten :as daten]))


(def storage-path "app-data")
(def storage-schlangen-path (str storage-path "/schlangen"))


(defn read-state-from-disk! []
  (let [file (-> storage-path (str "/state.edn") java.io.File.)
        state (or (files/read-edn file)
                  (daten/neues-system))]
    (reduce (fn [state schlange]
              (assoc-in state [:schlangen (-> schlange :id)] schlange))
            state
            (files/read-entities storage-schlangen-path))))


(defn write-state-to-disk! [state]
  (let [file (-> storage-path (str "/state.edn") java.io.File.)
        schlangen (-> state :schlangen vals)]
    (when-not (-> file .exists) (-> file .getParentFile .mkdirs))
    (files/write-edn file (-> state
                              (dissoc :schlangen)))
    (files/write-entities storage-schlangen-path schlangen)))


(txa/def-txa !system
  {:load-f (fn [_txa] (read-state-from-disk!))
   :store-f (fn [_txa value] (write-state-to-disk! value))})


(defn respond-with-schlange [schlange-id]
  (let [system (txa/read !system)
        schlange (get-in system [:schlangen schlange-id])]
    (str schlange)))


(defn respond-with-ticket [ticket-id]
  (let [system (txa/read !system)
        ticket (get-in system [:freie-tickets ticket-id])]
    (if ticket
      (-> ticket
          (assoc :eingecheckt? false)
          str)
      (let [schlange-id (get-in system [:ticket-id->schlange-id ticket-id])
            schlange (get-in system [:schlangen schlange-id])
            ticket (daten/finde-ticket-by-id (-> schlange :plaetze) ticket-id)]
        (-> ticket
            (assoc :eingecheckt? true)
            str)))))


(defn serve-ziehe-ticket [_context]
  (let [ticket-id (daten/neue-ticket-id)]
    (txa/transact-sync
     !system
     (fn [system] (daten/ziehe-ticket system ticket-id)))
    (respond-with-ticket ticket-id)))


(defn serve-eroeffne-schlange [context]
  (let [schlange-id (daten/neue-schlange-id)]
    (txa/transact-sync
     !system
     (fn [system] (daten/eroeffne-schlange system schlange-id)))
    (respond-with-schlange schlange-id)))


(defn serve-checke-ein [context]
  (let [params (-> context :http/request :params)
        schlange-id (-> params :schlange)
        ticket-nummer (-> params :ticket)]
    (txa/transact-sync
     !system
     (fn [system] (daten/checke-ein system schlange-id ticket-nummer)))
    (respond-with-schlange schlange-id)))


(defn serve-update-ticket-by-praxis [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))
        schlange-id (get-in (txa/read !system) [:ticket-id->schlange-id ticket-id])] ;; FIXME
    (txa/transact-sync
     !system
     (fn [system] (daten/update-ticket-by-praxis system ticket-id props)))
    (respond-with-schlange schlange-id)))


(defn serve-update-ticket-by-patient [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))]
    (txa/transact-sync
     !system
     (fn [system] (daten/update-ticket-by-patient system ticket-id props)))
    (respond-with-ticket ticket-id)))


(defn serve-ticket [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :id)]
    (respond-with-ticket ticket-id)))


(defn serve-schlange [context]
  (let [params (-> context :http/request :params)
        schlange-id (-> params :id)]
    (respond-with-schlange schlange-id)))


(defn serve-state [context]
  (str (txa/read !system)))


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
