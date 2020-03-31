(ns wartsapp.patient-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]
   [re-frame.core :as rf]
   [reagent.core :as r]

   [kcu.utils :as u]
   [kcu.ui :as ui]
   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]

   [kunagi-base-browserapp.notifications :as notifications]))


(defn Stepper [patient]
  [:> mui/Stepper
   {:alternative-label true
    :active-step (case (-> patient :patient/status)
                   :aufruf-bestaetigt 4
                   :aufgerufen 3
                   :eingecheckt 2
                   :frei 1
                   :entfernt nil)}
   [:> mui/Step
    [:> mui/StepLabel
     "Ticket ziehen"]]
   [:> mui/Step
    [:> mui/StepLabel
     "In Praxis einchecken"]]
   [:> mui/Step
    [:> mui/StepLabel
     "Aufgerufen werden"]]
   [:> mui/Step
    [:> mui/StepLabel
     "Auf den Weg machen"]]])

(defn Ticket-Unterwegs-Box []
  [muic/Card
   {:style {:background-color (theme/color-secondary-main)
            :color (theme/color-secondary-contrast)
            :text-align :center}}
   [:h3
    "Sie haben den Aufruf bestätigt"]
   [:p "Die Praxis erwartet Sie jetzt"]])

(defn Ticket-Aufgerufen-Box []
  [muic/Card
   {:style {:background-color (theme/color-secondary-main)
            :color (theme/color-secondary-contrast)
            :text-align :center}}
   [:h3
    "Sie wurden aufgerufen"]
   [:p "Bitte machen Sie sich auf den Weg zur Praxis"]
   [:> mui/Button
    {:variant :contained
     :color :primary
     :on-click #(rf/dispatch [:wartsapp/ich-bin-unterwegs-clicked])}
    "Ich bin unterwegs"]])

(defn Ticket-Warten-Box []
  [:div
   {:style {
            :text-align :center}}
   [:h3
    "Sie sind eingecheckt"]
   [:p "Bitte warten Sie auf den Aufruf"]])

(defn Ticket-Entfernt-Box []
  [:div
   {:style {
            :text-align :center}}
   [:h3
    "Die Praxis hat ihr Ticket entfernt"]
   [:p "Damit ist der Vorgang abgeschlossen"]])


(defn Call-To-Action-Box [patient]
  (case (-> patient :patient/status)
    :entfernt [Ticket-Entfernt-Box]
    :aufruf-bestaetigt [Ticket-Unterwegs-Box]
    :aufgerufen [Ticket-Aufgerufen-Box]
    :eingecheckt [Ticket-Warten-Box]
    nil))

(defn Nummer [nummer]
  [:h1
   {:style {:font-family :monospace
            :text-align :center
            :font-size "500%"
            :margin "0 0"
            :padding-top "30px"
            :padding-bottom "10px"}}
   nummer])


(defn Notification-Config []
  (let [!permission-granted? (r/atom (notifications/permission-granted?))]
    (fn []
      [:div
       (when-not @!permission-granted?
         [muic/Stack
          {:spacing (theme/spacing 1)
           :style {:text-align :center}}
          [:> mui/Button
           {:variant :contained
            :color :primary
            :on-click #(notifications/request-permission
                        (fn [result]
                          (tap> [::dbg ::permission-result result])
                          (reset! !permission-granted? (= "granted" result))))}
           "Popup Benachrichtigung aktivieren"]])])))


(defn Patient [patient]
  [muic/Stack
   {:spacing (theme/spacing 2)}
   [muic/Card
    {:elevation 0}
    [muic/Stack
     {:spacing (theme/spacing 2)}
     [Stepper patient]
     (when patient
       [muic/Stack
        {:spacing (theme/spacing 1)}
        [Call-To-Action-Box patient]
        [Nummer (-> patient :patient/nummer)]
        [Notification-Config]])]]
   (when-not (contains? #{:eingecheckt :aufgerufen} (-> patient :patient/status))
     [:> mui/Button
      {:variant :contained
       :color :primary
       :on-click #(rf/dispatch [:wartsapp/ticket-anfordern-clicked])}
      "Neues Ticket ziehen"])])


(ui/def-component Patient)