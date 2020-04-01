(ns wartsapp.dev-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]

   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]

   [wartsapp.manager]

   [kcu.utils :as u]
   [kcu.projector :as projector]
   [kcu.aggregator :as aggregator]))


(defn Basic-Commandflow []
  [:div
   "-"])


(defn Workarea []
  [muic/Stack-1
   [Basic-Commandflow]])
