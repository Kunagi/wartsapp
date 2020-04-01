(ns wartsapp.schlange-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]
   [re-frame.core :as rf]
   [reagent.core :as r]

   [kcu.utils :as u]
   [kcu.bapp :as bapp]
   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]))


(defn- pad-0 [i]
  (str
   (when (< i 10) "0")
   i))


(defn format-millis-just-time [millis]
  (when millis
    (let [date (-> millis js/Date.)]
      (str (pad-0 (-> date .getHours))
           ":"
           (pad-0 (-> date .getMinutes))))))


(defn TimeField [label millis]
  (if-not millis
    [:div]
    [:div
     {:style {:white-space :nowrap}}
     label ": "
     [:strong
      (format-millis-just-time millis)]]))


(defn Patient__Status [patient]
  [:div
   {:style {:display :grid
            :grid-template-columns "1fr 1fr 1fr 1fr"
            :grid-gap (theme/spacing 2)}}
   [TimeField "Eingecheckt" (-> patient :patient/eingecheckt)]
   [TimeField "Aufgerufen" (-> patient :patient/aufgerufen)]
   [TimeField "Unterwegs" (-> patient :patient/aufruf-bestaetigt)]])


(defn Patient__Identifikation [patient]
  [:div
   {:style {:display :grid
            :grid-template-columns "60px auto 60px"}}
   [:h3
    (-> patient :patient/nummer)]
   [:> mui/TextField
    {:label "Patient"
     :default-value (-> patient :patient/praxis-patient)
     :on-change #(rf/dispatch [:wartsapp/ticket-praxis-patient-changed
                               (-> patient :patient/id)
                               (-> % .-target .-value)])}]
   [:> mui/IconButton
    {:color :secondary
     :aria-label "Patient Entfernen"
     :on-click #(rf/dispatch [:wartsapp/ticket-entfernen-clicked (-> patient :patient/id)])}
    [:> icons/Delete]]])


(defn Patient [patient]
  [muic/Card
   [muic/Stack-1
    [Patient__Identifikation patient]
    [Patient__Status patient]
    (when-not (-> patient :patient/aufgerufen)
      [:> mui/Button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [:wartsapp/aufrufen-clicked (-> patient :id)])}
       "Aufrufen"])]])


(defn Schlange [schlange]
  [muic/Stack-1
   (for [patient (->> schlange
                      :plaetze
                      vals
                      (remove #(contains? % :patient/entfernt))
                      (sort-by :patient/eingecheckt))]
     ^{:key (-> patient :patient/id)}
     [Patient patient])])


(bapp/def-component Schlange)
