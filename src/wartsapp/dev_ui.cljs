(ns wartsapp.dev-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]

   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]

   [wartsapp.manager]

   [kcu.utils :as u]
   [kcu.projector :as projector]
   [kcu.aggregator :as aggregator]
   [kcu.devtools-ui :as dui]))


(defn Basic-Commandflow []
  [:div
   [dui/Aggregate-Command-Flow
    {:aggregator (aggregator/aggregator :wartsapp.manager)
     :projectors [(projector/projector :wartsapp.patient)]
     :commands [

                [:ziehe-nummer
                 {:patient/id "patient-1"}]

                [:checke-ein {:schlange/id "schlange-1"
                              :nummer "a1"}]

                [:rufe-auf {:patient/id "patient-1"}]

                [:bestaetige-aufruf {:patient/id "1"}]

                [:entferne-patient-von-schlange {:schlange/id "schlange-1"
                                                 :patient/id "patient-1"}]]}]])

(defn Workarea []
  [muic/Stack-1
   [Basic-Commandflow]])
