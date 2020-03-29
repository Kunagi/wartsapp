(ns wartsapp.dev-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]

   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]

   [wartsapp.aggregat]

   [kcu.utils :as u]
   [kcu.projector :as projector]
   [kcu.aggregator :as aggregator]
   [kcu.devtools-ui :as dui]))


(defn Schlange-Projection []
  [:div
   [dui/Projection-Event-Flow
    {:projector (projector/projector :wartsapp.schlange)
     :events [[:eroeffnet {:id "s1"
                           :time 1}]
              [:ticket-eingecheckt {:ticket {:id "t1"}
                                    :time 1}]
              [:ticket-geaendert {:ticket-id "t1"
                                  :props {:unterwegs 123}}]]}]])


(defn Basic-Commandflow []
  [:div
   [dui/Aggregate-Command-Flow
    {:aggregator (aggregator/aggregator :wartsapp.aggregat)
     :commands [[:eroeffne-schlange {:id "schlange-1"}]
                [:ziehe-ticket {:id "ticket-1"}]
                [:checke-ein {:schlange-id "schlange-1"
                              :ticket-nummer "abc"}]
                [:aendere-ticket {:id "ticket-1"
                                  :props {:aufgerufen 12345}}]]}]])


(defn Workarea []
  [muic/Stack-1
   [Basic-Commandflow]])
   ;; [Schlange-Projection]])
