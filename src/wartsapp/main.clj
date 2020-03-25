(ns wartsapp.main
  (:require
   [clojure.edn :as edn]
   [puget.printer :as puget]

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


(def storage-path "app-data")

(defn read-schlangen-from-disk! []
  (let [dir (-> storage-path (str "/schlangen") java.io.File.)]
    (when (-> dir .exists)
      (map (fn [file]
             (-> file slurp edn/read-string))
           (->> dir
                .listFiles
                (filter #(.endsWith (-> % .getName) ".edn")))))))


(defn read-state-from-disk! []
  (let [file (-> storage-path (str "/state.edn") java.io.File.)
        state (if (-> file .exists)
                (-> file slurp edn/read-string)
                (daten/neues-system))]
    (reduce (fn [state schlange]
              (assoc-in state [:schlangen (-> schlange :id)] schlange))
            state
            (read-schlangen-from-disk!))))


(defn write-schlange-to-disk! [schlange]
  (let [file (-> storage-path (str "/schlangen/" (get schlange :id) ".edn") java.io.File.)]
    (when-not (-> file .exists) (-> file .getParentFile .mkdirs))
    (spit file (puget/pprint-str schlange))))


(defn write-state-to-disk! [state]
  (let [file (-> storage-path (str "/state.edn") java.io.File.)
        schlangen (-> state :schlangen vals)]
    (when-not (-> file .exists) (-> file .getParentFile .mkdirs))
    (spit file (-> state
                   (dissoc :schlangen)
                   (puget/pprint-str)))
    (doseq [schlange schlangen]
      (write-schlange-to-disk! schlange))))


(defn on-agent-error [_agent ex]
  (tap> [:err ::on-db-agent-error ex]))


(defonce db (agent {}))
(defonce !state (agent (read-state-from-disk!)
                       :error-mode :continue
                       :error-handler on-agent-error))

(defn transact! [f]
  (send-off !state
            (fn [state]
              (let [state (f state)]
                (write-state-to-disk! state)
                state))))

(defn transact-sync! [f]
  (await (transact! f)))


(defn respond-with-schlange [schlange-id]
  (let [schlange (get-in @!state [:schlangen schlange-id])]
    (str schlange)))


(defn respond-with-ticket [ticket-id]
  (let [ticket (get-in @!state [:freie-tickets ticket-id])]
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


(defn serve-ziehe-ticket [_context]
  (let [ticket-id (daten/neue-ticket-id)]
    (transact-sync!
     (fn [system] (daten/ziehe-ticket system ticket-id)))
    (respond-with-ticket ticket-id)))


(defn serve-eroeffne-schlange [context]
  (let [schlange-id (daten/neue-schlange-id)]
    (transact-sync!
     (fn [system] (daten/eroeffne-schlange system schlange-id)))
    (respond-with-schlange schlange-id)))


(defn serve-checke-ein [context]
  (let [params (-> context :http/request :params)
        schlange-id (-> params :schlange)
        ticket-nummer (-> params :ticket)]
    (transact-sync!
     (fn [system] (daten/checke-ein system schlange-id ticket-nummer)))
    (respond-with-schlange schlange-id)))


(defn serve-update-ticket-by-praxis [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))
        schlange-id (get-in @!state [:ticket-id->schlange-id ticket-id])] ;; FIXME
     (transact-sync!
      (fn [system] (daten/update-ticket-by-praxis system ticket-id props)))
     (respond-with-schlange schlange-id)))


(defn serve-update-ticket-by-patient [context]
  (let [params (-> context :http/request :params)
        ticket-id (-> params :ticket)
        props (edn/read-string (-> params :props))]
    (transact-sync!
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
