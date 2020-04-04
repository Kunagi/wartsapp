(ns wartsapp.main
  (:require
   [re-frame.core :as rf]

   [kunagi-base.logging.tap]
   [kunagi-base.enable-asserts]

   [mui-commons.init :as init]

   [kcu.bapp :as bapp]
   [kcu.simulator-ui :as simulator-ui]

   [kunagi-base.modules.startup.api :as startup]

   [kunagi-base.appmodel :refer [def-module]]
   [kunagi-base-browserapp.modules.desktop.model :refer [def-page]]

   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.fonts :as fonts]
   [wartsapp.ui :as ui :refer [Desktop]]
   [wartsapp.patient-ui :as patient-ui]
   [wartsapp.schlange-ui :as schlange-ui]
   [wartsapp.dev-ui :as dev-ui]))


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
   :page/workarea [(fn [] [patient-ui/Workarea])]})

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

(def-page
  {:page/id ::dev
   :page/ident :dev
   :page/title-text "Entwicklertests"
   :page/workarea [(fn [] [dev-ui/Workarea])]})


(defn mount-app []
  (init/mount-app Desktop))

(defn init []
  (startup/install-serviceworker!)
  (fonts/install!)
  ;(desktop/install-error-handler)
  (startup/start!
   {:app/info appinfo})
  (bapp/init!)
  (mount-app))



(defn shadow-after-load []
  (mount-app))
