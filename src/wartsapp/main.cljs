(ns wartsapp.main
  (:require
   [re-frame.core :as rf]

   [kunagi-base.logging.tap]
   [kunagi-base.enable-asserts]

   [mui-commons.init :as init]

   [kunagi-base.modules.startup.api :as startup]
   [kunagi-base.appmodel :refer [def-module]]
   [kunagi-base-browserapp.modules.desktop.model :refer [def-page]]
   [kunagi-base-browserapp.modules.desktop.api :as desktop]
   [kunagi-base-browserapp.modules.assets.api :as assets]
   [kunagi-base-browserapp.modules.assets.model :refer [def-asset-pool]]

   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.fonts :as fonts]
   [wartsapp.ui :as ui :refer [Desktop]]

   [wartsapp.daten :as daten]))

(def-module
  {:module/id ::demo-browserapp})

(def-asset-pool
  {:asset-pool/id ::ticket
   :asset-pool/ident :wartsapp/ticket
   :asset-pool/localstorage? true
   :asset-pool/load-on-startup ["myticket.edn"]})

(def-asset-pool
  {:asset-pool/id ::schlange
   :asset-pool/ident :wartsapp/schlange
   :asset-pool/localstorage? true
   :asset-pool/load-on-startup ["myschlange.edn"]})

(def-page
  {:page/id ::index
   :page/ident :index
   :page/title-text (-> appinfo :project :name)
   :page/workarea [(fn [] (ui/Index-Workarea))]})

(def-page
  {:page/id ::ticket
   :page/ident :ticket
   :page/title-text "Mein Ticket"
   :page/workarea [(fn [] (ui/Ticket-Workarea))]})

(def-page
  {:page/id ::schlange
   :page/ident :schlange
   :page/title-text "Warteschlange"
   :page/workarea [(fn [] (ui/Schlange-Workarea))]})

(def-page
  {:page/id ::legal
   :page/ident :legal
   :page/title-text "Datenschutzerklärung"
   :page/workarea [(fn [] (ui/Legal-Workarea))]})


(defn poll! []
  (js/setTimeout (fn []
                   (rf/dispatch [:wartsapp/poll-ticket])
                   (rf/dispatch [:wartsapp/poll-schlange])
                   (poll!))
                 1000))


(defn mount-app []
  (init/mount-app Desktop))

(defn init []
  (startup/install-serviceworker!)
  (fonts/install!)
  (desktop/install-error-handler)
  (startup/start!
   {:app/info appinfo})
  (poll!)
  (mount-app))

(defn shadow-after-load []
  (mount-app))
