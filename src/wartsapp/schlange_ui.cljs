(ns wartsapp.schlange-ui
  (:require
   ["@material-ui/core" :as mui]
   ["@material-ui/icons" :as icons]
   [re-frame.core :as rf]
   [reagent.core :as r]

   [kcu.utils :as u]
   [kcu.butils :as bu]
   [kcu.bapp :as bapp]
   [kcu.form-ui :as form-ui]
   [mui-commons.components :as muic]
   [mui-commons.theme :as theme]
   [clojure.string :as str]))


(defn dispatch-on-server
  ([command-name schlange]
   (dispatch-on-server command-name schlange nil))
  ([command-name schlange options]
   (bapp/dispatch-on-server
    (merge
     {:command/name command-name
      :schlange/id (-> schlange :projection/id)}
     options))))


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


(defonce PATIENTEN-INFOS (bu/durable-ratom :patienten-infos {}))


(defn patient-info [patient]
  (get @PATIENTEN-INFOS (-> patient :patient/id)))


(defn Patient__Identifikation__Feld [options text]
  [:div
   (merge options
          {:style {:border "1px solid rgba(0, 0, 0, 0.38)"
                   :border-radius "3px"
                   :padding 14
                   :cursor :pointer}})
   (if (str/blank? text)
     [:span
      {:style {:color "#aaa"}}
      "Patient..."]
     text)])


(defn Patient__Identifikation [schlange patient]
  [:div
   {:style {:display :grid
            :grid-template-columns "60px auto 60px"}}
   [:h3
    (-> patient :patient/nummer)]
   [form-ui/TextFieldDialog
    {:title "Patient"
     :cancel-button-text "Abbrechen"
     :submit-button-text "Speichern"
     :trigger [Patient__Identifikation__Feld {} (patient-info patient)]
     :value (patient-info patient)
     :on-submit #(swap! PATIENTEN-INFOS assoc
                        (-> patient :patient/id) (-> @% :value))}]
   [:> mui/IconButton
    {:color :secondary
     :aria-label "Patient Entfernen"
     :on-click #(dispatch-on-server :wartsapp/entferne-patient-von-schlange
                                    schlange
                                    {:patient/id (-> patient :patient/id)})}
    [:> icons/Delete]]])


(defn Patient [schlange patient]
  [muic/Card
   [muic/Stack-1
    [Patient__Identifikation schlange patient]
    [Patient__Status patient]
    (when-not (-> patient :patient/aufgerufen)
      [:> mui/Button
       {:variant :contained
        :color :primary
        :on-click #(dispatch-on-server :wartsapp/rufe-auf
                                       schlange
                                       {:patient/id (-> patient :patient/id)})}
       "Aufrufen"])]])


(defn Checkin [schlange]
  [form-ui/TextFieldDialog
   {:title "Patientin einchecken"
    :text "Bitte die Nummer eingeben, welche die Patientin auf ihrem Smartphone gezogen hat."
    :cancel-button-text "Abbrechen"
    :submit-button-text "Einchecken"
    :trigger [:> mui/Button
              {:variant :contained
               :color :secondary}
              "Patientin einchecken ..."]
    :text-field {:label "Nummer"}
    :on-submit (fn [STATE {:keys [close block unblock]}]

                 (bapp/dispatch-on-server
                  {:command/name :wartsapp/checke-ein
                   :schlange/id (-> schlange :projection/id)
                   :nummer (-> @STATE :value)}
                  (fn [result]
                    (if (-> result :rejected?)
                      (unblock
                       {:error-text (or (-> result :text)
                                        "Checkin fehlgeschlagen")})
                      (close))))

                 (block)

                 false)}])


(defn Schlange [schlange]
  [muic/Stack-1
   ;; [muic/Data schlange]

   [Checkin schlange]

   (let [patienten (->> schlange
                        :plaetze
                        vals
                        (remove #(contains? % :patient/entfernt))
                        (sort-by :patient/eingecheckt))]
     (if (empty? patienten)
       [:div "Keine Patienten auf der Warteliste"]
       (for [patient patienten]
         ^{:key (-> patient :patient/id)}
         [Patient schlange patient])))])


(bapp/def-component Schlange)


(defn Workarea []
  (let [schlange-id (bapp/durable-uuid "schlange-id")
        schlange (bapp/projection :wartsapp.schlange schlange-id)]
    (bapp/subscribe-on-server {:query/name :system/projection
                               :projection/projector :wartsapp.schlange
                               :projection/id schlange-id})
    [muic/ErrorBoundary
     [Schlange schlange]]))
