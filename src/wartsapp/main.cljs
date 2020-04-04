(ns wartsapp.main
  (:require
   [re-frame.core :as rf]

   [kunagi-base.logging.tap]
   [kunagi-base.enable-asserts]

   [mui-commons.init :as init]

   [kcu.butils :as bu]
   [kcu.bapp :as bapp]
   [kcu.simulator-ui :as simulator-ui]

   [kunagi-base.modules.startup.api :as startup]

   [kunagi-base.appmodel :refer [def-module]]
   [kunagi-base-browserapp.modules.desktop.model :refer [def-page]]

   [wartsapp.patient]
   [wartsapp.schlange]
   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.fonts :as fonts]
   [wartsapp.ui :as ui :refer [Desktop]]
   [wartsapp.patient-ui :as patient-ui]
   [wartsapp.schlange-ui :as schlange-ui]))


(defonce patient-id (bapp/durable-uuid "patient-id"))


(def-module
  {:module/id ::demo-browserapp})

(def-page
  {:page/id ::index
   :page/ident :index
   :page/title-text (-> appinfo :project :name)
   :page/workarea [(fn [] (ui/Index-Workarea))]})

(def-page
  {:page/id ::patient
   :page/ident :patient
   :page/title-text "Meine Wartenummer"
   :page/workarea [(fn [] [patient-ui/Workarea patient-id])]})

(def-page
  {:page/id ::schlange
   :page/ident :schlange
   :page/title-text "Warteschlange"
   :page/workarea [(fn [] (schlange-ui/Workarea))]})

(def-page
  {:page/id ::legal
   :page/ident :legal
   :page/title-text "Datenschutzerklärung"
   :page/workarea [(fn [] (ui/Legal-Workarea))]})

(def-page
  {:page/id ::simulator
   :page/ident :simulator
   :page/title-text "Simulator"
   :page/workarea [(fn [args] [simulator-ui/Workarea args])]})


(defn show-notification-wenn-aufgerufen [patient]
  (when-let [aufgerufen (-> patient :patient/aufgerufen)]
    (bu/show-notification-once
     [(-> patient :id) aufgerufen]
     "Sie wurden aufgerufen"
     {:body "Bitte machen Sie sich auf den Weg zur Praxis."
      :icon "/img/app-icon_128.png"
      :lang "de"
      :tag (str "aufgerufen-" (-> patient :id))
      :vibrate [300 200 100 200 300]
      :requireInteraction true
      :actions [{:action "show:/ui/patient"
                 :title "Zur Wartenummer"}]})))


(defn mount-app []
  (init/mount-app Desktop))


(defn init []
  (startup/install-serviceworker!)
  (fonts/install!)
  (desktop/install-error-handler)
  (startup/start!
   {:app/info appinfo})
  (bapp/init!)
  (add-watch (bapp/projection-bucket :wartsapp.patient patient-id)
             ::notification
             (fn [_ _ _ patient]
               (show-notification-wenn-aufgerufen patient)))
  (mount-app))



(defn shadow-after-load []
  (mount-app))
