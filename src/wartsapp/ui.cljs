(ns wartsapp.ui
  (:require
     [cljs.reader :as reader]
     [clojure.string :as string]
     [goog.object :as gobj]
     ["@material-ui/core/colors" :as mui-colors]
     ["@material-ui/core" :as mui]
     ["@material-ui/icons" :as icons]
     [reagent.core :as r]
     [re-frame.core :as rf]
     [ajax.core :as ajax]

     [kcu.utils :as u]
     [kcu.bapp :as bapp]
     [mui-commons.components :as muic]
     [mui-commons.theme :as theme]
     [kcu.bapp-ui :as bapp-ui]
     [kunagi-base-browserapp.modules.desktop.components :as desktop]
     [kunagi-base-browserapp.modules.assets.api :as assets]
     [kunagi-base-browserapp.notifications :as notifications]


     [wartsapp.patient-ui :as patient-ui]
     [wartsapp.schlange-ui :as schlange-ui]

     [wartsapp.appinfo :refer [appinfo]]
     [wartsapp.datenschutzerklaerung :as dse]
     [wartsapp.daten :as daten]))


;;; data


(bapp/def-lense wartsapp
  {})


(bapp/def-lense praxis
  {:parent wartsapp})

(bapp/def-lense checkin-ticket-nummer
  {:parent praxis})

(bapp/def-lense schlange-lense
  {:parent praxis
   :durable? true
   :server-subscription-query (fn [db lense]
                                (when-let [schlange-id (:id (bapp/read db lense))]
                                  [:wartsapp.main/schlange-fuer-praxis
                                   {:schlange-id schlange-id}]))})

(bapp/def-lense patient
  {:parent wartsapp})

(declare show-notification-wenn-aufgerufen)

(bapp/def-lense ticket-lense
  {:parent patient
   :durable? true
   :setter (fn [value]
             (show-notification-wenn-aufgerufen value)
             value)
   :server-subscription-query (fn [db lense]
                                (when-let [ticket-id (:id (bapp/read db lense))]
                                  [:wartsapp.main/ticket-fuer-patient
                                   {:ticket-id ticket-id}]))})


(defn post-command!
  [endpoint params]
  (ajax/GET endpoint {:params params}))


(defn reg-ajax-event
  [event-id
   {:keys [endpoint params]}]
  (rf/reg-event-db
   event-id
   (fn [db event]
     (ajax/GET endpoint
               {:params (if (fn? params)
                          (params db event)
                          params)})
     db)))


(rf/reg-event-db
 :wartsapp/eroeffne-schlange-clicked
 (fn [db _]
   (let [schlange-id (daten/neue-schlange-id)
         schlange (daten/leere-schlange schlange-id)]
     (post-command! "/api/eroeffne-schlange" {:schlange-id schlange-id})
     (bapp/reset db schlange-lense schlange))))


(reg-ajax-event
 :wartsapp/aufrufen-clicked
 {:endpoint "/api/update-ticket-by-praxis"
  :params (fn [_db [_ ticket-id]]
            {:ticket ticket-id
             :props (str {:aufgerufen (daten/ts)})})})


(reg-ajax-event
 :wartsapp/ticket-entfernen-clicked
 {:endpoint "/api/update-ticket-by-praxis"
  :params (fn [_db [_ ticket-id]]
             {:ticket ticket-id
              :props (str {:entfernt (daten/ts)})})})


;; FIXME store names in separate lense
(rf/reg-event-db
 :wartsapp/ticket-praxis-patient-changed
 (fn [db [_ ticket-id value]]
   (let [schlange (bapp/read db schlange-lense)
         schlange (daten/update-ticket schlange ticket-id {:praxis-patient value})]
     (ajax/GET "/api/update-ticket-by-praxis"
               {:params {:ticket ticket-id
                         :props (str {:praxis-patient value})}})
     (bapp/reset db schlange-lense schlange))))


(rf/reg-event-db
 :wartsapp/checkin-clicked
 (fn [db _]
   (let [ticket-nummer (bapp/read db checkin-ticket-nummer)
         schlange-id (:id (bapp/read db schlange-lense))]
     (ajax/GET "/api/checke-ein"
               {:params {:schlange schlange-id
                         :ticket ticket-nummer}
                :handler (fn [response]
                           (let [schlange (reader/read-string response)]
                             (when-not (-> schlange :checkin-fehler)
                               (bapp/dispatch-reset checkin-ticket-nummer ""))))})
     db)))


(rf/reg-event-db
 :wartsapp/ticket-anfordern-clicked
 (fn [db _]
   (let [ticket-id (daten/neue-ticket-id)
         ticket {:id ticket-id}]
     (post-command! "/api/ziehe-ticket" {:ticket-id ticket-id})
     (bapp/reset db ticket-lense ticket))))


(reg-ajax-event
 :wartsapp/ich-bin-unterwegs-clicked
 {:endpoint "/api/update-ticket-by-patient"
  :params (fn [db _]
            {:ticket (:id (bapp/read db ticket-lense))
             :props (str {:unterwegs (daten/ts)})})})


(defn show-notification-wenn-aufgerufen [ticket]
  (when (-> ticket :aufgerufen)
    (let [localstorage-key (str "notification-aufgerufen-" (-> ticket :id))]
      (when-not (.getItem (.-localStorage js/window) localstorage-key)
        (notifications/show-notification
         "Sie wurden aufgerufen"
         {:body "Bitte machen Sie sich auf den Weg zur Praxis."
          :icon "/img/app-icon_128.png"
          :lang "de"
          :tag (str "aufgerufen-" (-> ticket :id))
          :vibrate [300 200 100 200 300]
          :requireInteraction true
          :actions [{:action "show:/ui/ticket"
                     :title "Zum Warteticket"}]})
        (.setItem (.-localStorage js/window) localstorage-key (daten/ts))))))


;;; Warteschlange

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


(defn Schlange-Ticket [ticket]
  (if-not (ticket :entfernt)
    [muic/Card
     [muic/Stack
      {:spacing (theme/spacing 1)}
      [:div
       {:style {:display :grid
                :grid-template-columns "60px auto 60px"}}
       [:h3
        (-> ticket :nummer)]
       [:> mui/TextField
        {:label "Patient"
         :default-value (-> ticket :praxis-patient)
         :on-change #(rf/dispatch [:wartsapp/ticket-praxis-patient-changed
                                   (-> ticket :id)
                                   (-> % .-target .-value)])}]
       [:> mui/IconButton
        {:color :secondary
         :aria-label "Ticket Entfernen"
         :on-click #(rf/dispatch [:wartsapp/ticket-entfernen-clicked (-> ticket :id)])}
        [:> icons/Delete]]]
      [:div
       {:style {:display :grid
                :grid-template-columns "1fr 1fr 1fr 1fr"
                :grid-gap (theme/spacing 2)}}
       [TimeField "Eingecheckt" (-> ticket :eingecheckt)]
       [TimeField "Aufgerufen" (-> ticket :aufgerufen)]
       [TimeField "Unterwegs" (-> ticket :unterwegs)]]
      (when-not (-> ticket :aufgerufen)
        [:> mui/Button
         {:variant :contained
          :color :primary
          :on-click #(rf/dispatch [:wartsapp/aufrufen-clicked (-> ticket :id)])}
         "Aufrufen"])]]))

    ;; [muic/Data ticket]]])


(defn Schlange-Checkin [schlange]
  [muic/Card
   [muic/Stack
    {:spacing (theme/spacing 1)}
    [:h3 "Ticket einchecken"]
    [:> mui/TextField
     {:label "Ticket-Nummer des Patienten"
      :value (or (bapp/subscribe checkin-ticket-nummer) "")
      :on-change #(bapp/dispatch-reset checkin-ticket-nummer
                                       (-> % .-target .-value))
      :on-key-down #(when (= 13 (-> % .-keyCode))
                      (rf/dispatch [:wartsapp/checkin-clicked]))
      :error (boolean (-> schlange :checkin-fehler))
      :helper-text (-> schlange :checkin-fehler)}]
    [:> mui/Button
     {:variant :contained
      :color :primary
      :on-click #(rf/dispatch [:wartsapp/checkin-clicked])}
     "Einchecken"]]])


(defn Schlange-Workarea []
  (if-let [schlange (bapp/subscribe schlange-lense)]
    [:div
     {:style {:display :grid
              :grid-template-columns "2fr 1fr"
              :grid-gap (theme/spacing 1)}}
     [:div
      ;; [:h3
      ;;  "Warteschlange "
      ;;  (-> schlange :bezeichnung)
      ;;  " #" (-> schlange :id)]
      [muic/Stack
       {:spacing (theme/spacing 1)}
       (for [ticket (-> schlange :plaetze)]
         ^{:key (-> ticket :id)}
         [Schlange-Ticket ticket])]]
     [:div
      [Schlange-Checkin schlange]]]
     ;; [muic/Card
     ;;  [:div "Debug"]
     ;;  [muic/Data schlange]]]
    [:> mui/Button
     {:variant :contained
      :color :primary
      :on-click #(rf/dispatch [:wartsapp/eroeffne-schlange-clicked])}
     "Neue Warteschlange eröffnen"]))





;;; Ticket (Patient Ansicht)

(defn Ticket-Stepper [ticket]
  [:> mui/Stepper
   {:alternative-label true
    :active-step (cond
                   (not ticket) 0
                   (-> ticket :unterwegs) 4
                   (-> ticket :aufgerufen) 3
                   (-> ticket :eingecheckt?) 2
                   :else 1)}
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

(defn Ticket-Call-To-Action-Box [ticket]
  (cond
    (-> ticket :entfernt) [Ticket-Entfernt-Box]
    (-> ticket :unterwegs) [Ticket-Unterwegs-Box]
    (-> ticket :aufgerufen) [Ticket-Aufgerufen-Box]
    (-> ticket :eingecheckt?) [Ticket-Warten-Box]
    :else nil))

(defn Ticket-Nummer [nummer]
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


(defn Ticket []
  (let [ticket (bapp/subscribe ticket-lense)]
    [muic/Stack
     {:spacing (theme/spacing 2)}
     [muic/Card
      {:elevation 0}
      [muic/Stack
       {:spacing (theme/spacing 2)}
       [Ticket-Stepper ticket]
       (when ticket
         [muic/Stack
          {:spacing (theme/spacing 1)}
          [Ticket-Call-To-Action-Box ticket]
          [Ticket-Nummer (-> ticket :nummer)]
          [Notification-Config]])]]
     (when (or (-> ticket :unterwegs)
               (-> ticket :entfernt)
               (not (-> ticket :eingecheckt)))
       [:> mui/Button
        {:variant :contained
         :color :primary
         :on-click #(rf/dispatch [:wartsapp/ticket-anfordern-clicked])}
        "Neues Ticket ziehen"])]))


(defn Ticket-Workarea []
  [:div
   [Ticket]])



;;; Index

(defn Info-Card []
  [muic/Card
   [:div
    {:style {:text-align :center
             :margin-bottom (theme/spacing 1)}}
    "Ein Projekt von "
    [:> mui/Link
     {:href "http://koczewski.de"
      :target :_blank}
     "Witoslaw Koczewski"]
    ", "
    [:> mui/Link
     {:href "http://fabianhager.de"
      :target :_blank}
     "Fabian Hager"]
    ", "
    [:> mui/Link
     {:href "https://www.linkedin.com/in/artjom-weyand-74437a155/"
      :target :_blank}
     "Artjom Weyand"]
    " und "
    [:> mui/Link
      {:href "mailto:kacper@grubalski.de"
       :target :_blank}
     "Kacper Grubalski"]
    " beim "
    [:div
     {:style {:text-align :center
              :margin-top (theme/spacing 4)
              :margin-bottom (theme/spacing 3)}}
     [:img
      {:src "/img/hackathon.jpg"
       :style {:max-width "350px"}}]]]
   [:p
    "Wenn Patienten eine Praxis aufsuchen, müssen sie in der Regel bis zur Behandlung im Wartezimmer Platz nehmen. Hier ist das Ansteckungsrisiko jedoch besonders hoch."]
   [:p
    "Um dieses Risiko zu reduzieren, sollen die Patienten in einem digitalen Wartezimmer warten und sich dabei außerhalb der Praxis aufhalten, zum Beispiel im eigenen Auto."]])


(defn Index-Workarea []
  [:div.Index-Workarea
   [muic/Stack
    {:spacing (theme/spacing 2)}
    [:div
     {:style {:display :grid
              :grid-template-columns "1fr 1fr"
              :grid-gap (theme/spacing 2)}}
     [:> mui/Card
      [:> mui/CardActionArea
       {:href "patient"}
       [:> mui/CardContent
        {:style {:text-align :center}}
        [:h3 "Ich bin Patient"]
        [:p "Ich möchte einen digitalen Warteplatz"]]]]
     [:> mui/Card
      [:> mui/CardActionArea
       {:href "schlange"}
       [:> mui/CardContent
        {:style {:text-align :center}}
        [:h3 "Ich bin Ärztin"]
        [:p "Ich möchte ein digitales Wartezimmer anbieten"]]]]]
    [Info-Card]
    [:div
     {:style {:text-align :right}}
     [:> mui/Link
      {:href "legal"}
      "Impressum · Datenschutzerklärung"]]]])


;;; Legal


(defn Legal-Workarea []
  [muic/Stack
   {:spacing (theme/spacing 2)}
   [muic/Card
    [:h2 "Impressum"]
    (into
     [:p]
     (interpose
      [:br]
      (-> appinfo :legal :vendor (string/split #"\n"))))]
   [muic/Card
    [:div
     {:dangerouslySetInnerHTML {:__html dse/html}}]]])


;;; Desktop

(defn agreement-accepted? [] true)

(defn HomeIcon []
  [:img
   {:src "/img/appbar-icon.png"
    :alt "App Icon"
    :width 32
    :height 32}])

(defn AppBar []
  [:> mui/AppBar
   ;;{:position :fixed}
   [:> mui/Toolbar

    (if (agreement-accepted?)
      [desktop/MainNavIconButtonSwitch
       [HomeIcon]]
      [HomeIcon])

    [desktop/PageTitleSwitch :h1]

    [:div
     {:style {:flex-grow 1
              :min-width "1rem"}}]

    (into [:div
           {:style {:display :flex}}]
          [[bapp-ui/SenteStatusIndicator]])

    (if (agreement-accepted?)
      [desktop/AppBarToolbar])]])
      ;;[agreement/Toolbar])]])



;; https://material.io/resources/color/#!/?view.left=0&view.right=0&primary.color=FFA726&secondary.color=33691E

(def palette
  (merge
   theme/default-palette
   {:primary {:main (gobj/get (.-teal mui-colors) 800)}
    :secondary {:main (gobj/get (.-pink mui-colors) 600)}
    :text-color (gobj/get (.-grey mui-colors) 300)
    :background {:default "#E1E2E1"}

    :greyed "#aaa"}))


(def theme
  (merge
   theme/default-theme
   {:palette palette}))

(defn css []
  {
   :font-family "'Montserrat', sans-serif"
   :line-height 1.7
   "& .nowrap" {:white-space :nowrap}
   "& .tc" {:text-align :center}
   "& .b" {:font-weight :bold}
   "& .Lesetext" {:column-width "400px"}
   "& .Lesetext h2, .Lesetext h3, .Lesetext h4" {:column-span :all}
   ;; "& .MuiContainer-root" {:padding-bottom "24px"}
   "& .PageTitle .MuiTypography-h1" {:font-family "'Montserrat', sans-serif"
                                     :font-size "22px"
                                     :font-weight 400
                                     :letter-spacing  ".1em"
                                     :margin-left "0.5em"}
   "& .MuiTypography-h6" {:font-family "'Montserrat', sans-serif"
                          :font-weight 500
                          :letter-spacing  ".05em"}
   "& .MuiTypography-body2" {:font-family "'Montserrat', sans-serif"}
   "& .MuiButtonBase-root" {:font-family "'Montserrat', sans-serif"
                            :font-size "0.875rem"
                            :line-height 1.7}
   "& .MuiCardMedia-root" {:filter "grayscale(0.7)"}
   "& .MuiStepper-root" {:padding 0}
   "& .MuiStepLabel-label" {:font-size "60%"}
   ;; "& .MuiCardContent-root" {:font-family "'Montserrat', sans-serif"}
   "& .ContentFont" {:font-family "'Montserrat', sans-serif"
                     :line-height "150%"}})


(defn Footer []
  [:div
   {:style {:margin-bottom 45}}
   ;; [:div.DEBUG
   ;;  {:style {:padding "2em"}}
   ;;  [muic/Card [muic/Data (bapp/subscribe ticket-lense)]]
   ;;  [:hr]
   ;;  [muic/Card [muic/Data (bapp/subscribe bapp/conversation)]]]
   [:div
    {:style {:color "#aaa"
             :padding (theme/spacing)
             :text-align :right
             :font-size "10px"}}
    [:div
     [:> mui/Button
      {:href "debug"
       :size :small}
      " "]]
    [:div
     "v" (-> appinfo :release :major) "." (-> appinfo :release :minor)
     " | build " (-> appinfo :build-time)
     " | Kunagi Team (WirVsVirus Hackathon)"]]])


(defn Desktop []
  [desktop/Desktop
   {:css (css)
    :app-bar [AppBar]
    :font-family "'Montserrat', sans-serif"
    :document-title-suffix "WartsApp"
    :footer [Footer]}])


(theme/set-theme! theme)
