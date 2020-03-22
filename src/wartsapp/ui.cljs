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

     [mui-commons.components :as muic]
     [mui-commons.theme :as theme]
     [kunagi-base-browserapp.modules.desktop.components :as desktop]
     [kunagi-base-browserapp.modules.assets.api :as assets]
     [kunagi-base-browserapp.notifications :as notifications]

     [wartsapp.appinfo :refer [appinfo]]
     [wartsapp.datenschutzerklaerung :as dse]
     [wartsapp.daten :as daten]))


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
  [muic/Card
   [muic/Stack
    {:spacing (theme/spacing 1)}
    [:div
     {:style {:display :grid
              :grid-template-columns "60px auto"}}
     [:h3
      (-> ticket :nummer)]
     [:> mui/TextField
      {:label "Patient"
       :default-value (-> ticket :praxis-patient)
       :on-change #(rf/dispatch [:wartsapp/ticket-praxis-patient-changed
                                 (-> ticket :id)
                                 (-> % .-target .-value)])}]]
    [:div
     {:style {:display :grid
              :grid-template-columns "1fr 1fr 1fr"
              :grid-gap (theme/spacing 2)}}
     [TimeField "Eingecheckt" (-> ticket :eingecheckt)]
     [TimeField "Aufgerufen" (-> ticket :aufgerufen)]]
    (when-not (-> ticket :aufgerufen)
      [:> mui/Button
       {:variant :contained
        :color :primary
        :on-click #(rf/dispatch [:wartsapp/aufrufen-clicked (-> ticket :id)])}
       "Aufrufen"])]])
    ;; [muic/Data ticket]]])


(defn Schlange-Checkin [schlange]
  [muic/Card
   [muic/Stack
    {:spacing (theme/spacing 1)}
    [:h3 "Ticket einchecken"]
    [:> mui/TextField
     {:label "Ticket-Nummer des Patienten"
      :value @(rf/subscribe [:wartsapp/checkin-ticket-nummer])
      :on-change #(rf/dispatch [:wartsapp/checkin-ticket-nummer-changed
                                (-> % .-target .-value)])
      :error (-> schlange :checkin-fehler)
      :helper-text (-> schlange :checkin-fehler)}]
    [:> mui/Button
     {:variant :contained
      :color :primary
      :on-click #(rf/dispatch [:wartsapp/checkin-clicked])}
     "Einchecken"]]])


(defn Schlange-Workarea []
  (if-let [schlange (get-in @(rf/subscribe [:app/db])
                          [:assets/asset-pools :wartsapp/schlange "myschlange.edn"])]
    ;; FIXME
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



(rf/reg-event-db
 :wartsapp/eroeffne-schlange-clicked
 (fn [db _]
   (ajax/GET "/api/eroeffne-schlange"
             {:handler (fn [response]
                         (let [schlange (reader/read-string response)]
                           (rf/dispatch [:wartsapp/schlange-erhalten schlange])))
              :error-handler #(js/console.log "ERROR" %)})
   db))
   ;; (let [schlange-nummer (or (get-in db [:wartsapp :praxis :schlange-nummer])
   ;;                           "WS1")
   ;;       asset-path (str schlange-nummer ".edn")
   ;;       schlange (or (assets/asset db :wartsapp/schlangen asset-path)
   ;;                    (daten/leere-schlange))]
   ;;   (-> db
   ;;       (assoc-in [:wartsapp :praxis :schlange-nummer] schlange-nummer)
   ;;       (assets/set-asset :wartsapp/schlangen asset-path schlange)))))


(rf/reg-event-db
 :wartsapp/aufrufen-clicked
 (fn [db [_ ticket-id]]
   (ajax/GET "/api/update-ticket-by-praxis"
             {:params {:ticket ticket-id
                       :props (str {:aufgerufen (daten/ts)})}
              :handler (fn [response]
                         (let [schlange (reader/read-string response)]
                           (rf/dispatch [:wartsapp/schlange-erhalten schlange])))
              :error-handler #(js/console.log "ERROR" %)})
   db))

(rf/reg-event-db
 :wartsapp/schlange-erhalten
 (fn [db [_ ticket]]
   (assets/set-asset db :wartsapp/schlange "myschlange.edn" ticket)))

(rf/reg-event-db
 :wartsapp/checkin-ticket-nummer-changed
 (fn [db [_ nummer]]
   (assoc-in db [:wartsapp :praxis :checkin-ticket-nummer] nummer)))

(rf/reg-event-db
 :wartsapp/ticket-praxis-patient-changed
 (fn [db [_ ticket-id value]]
   (let [schlange (get-in db [:assets/asset-pools :wartsapp/schlange "myschlange.edn"])
         schlange (daten/update-ticket schlange ticket-id {:praxis-patient value})]
     (ajax/GET "/api/update-ticket-by-praxis"
               {:params {:ticket ticket-id
                         :props (str {:praxis-patient value})}
                :handler (fn [response]
                           (let [schlange (reader/read-string response)]
                             (rf/dispatch [:wartsapp/schlange-erhalten schlange])))
                :error-handler #(js/console.log "ERROR" %)})
     (assets/set-asset db :wartsapp/schlange "myschlange.edn" schlange))))

(rf/reg-sub
 :wartsapp/checkin-ticket-nummer
 (fn [db]
   (get-in db [:wartsapp :praxis :checkin-ticket-nummer])))


(rf/reg-event-db
 :wartsapp/checkin-clicked
 (fn [db _]
   (let [ticket-nummer (get-in db [:wartsapp :praxis :checkin-ticket-nummer])
         schlange-id (get-in db [:assets/asset-pools :wartsapp/schlange "myschlange.edn" :id])]
     (ajax/GET "/api/checke-ein"
               {:params {:schlange schlange-id
                         :ticket ticket-nummer}
                :handler (fn [response]
                           (let [schlange (reader/read-string response)]
                             (rf/dispatch [:wartsapp/schlange-erhalten schlange])))
                :error-handler #(js/console.log "ERROR" %)})
     db)))
   ;; (let [ticket-nummer (get-in db [:wartsapp :praxis :checkin-ticket-nummer])
   ;;       schlange-nummer (get-in db [:wartsapp :praxis :schlange-nummer])
   ;;       schlange (assets/asset db :wartsapp/schlangen (str schlange-nummer ".edn"))
   ;;       freie-tickets (get-in db [:assets/asset-pools :wartsapp/tickets])
   ;;       schlange (daten/einchecken schlange freie-tickets ticket-nummer)
   ;;       ticket-nummer (if (-> schlange :checkin-fehler)
   ;;                       ticket-nummer
   ;;                       "")]
   ;;   (-> db
   ;;       (assets/set-asset :wartsapp/schlangen (str schlange-nummer ".edn") schlange)
   ;;       (assoc-in [:wartsapp :praxis :checkin-ticket-nummer] ticket-nummer)))))


(rf/reg-sub
 :wartsapp/schlange
 (fn [db _]
   (let [schlange-nummer (get-in db [:wartsapp :praxis :schlange-nummer])]
     (-> db
         (assets/asset , :wartsapp/schlangen (str schlange-nummer ".edn"))))))


;;; Ticket (Patient Ansicht)

(defn Ticket-Stepper [ticket]
  [:> mui/Stepper
   {:alternative-label true
    :active-step (cond
                   (not ticket) 0
                   (-> ticket :aufgerufen) 3
                   (-> ticket :eingecheckt?) 2
                   :else 1)}
   [:> mui/Step
    [:> mui/StepLabel
     "Ticket ziehen"]]
   [:> mui/Step
    [:> mui/StepLabel
     "Einchecken"]]
   [:> mui/Step
    [:> mui/StepLabel
     "Aufgerufen werden"]]])

(defn Ticket-Aufgerufen-Box []
  [muic/Card
   {:style {:background-color (theme/color-secondary-main)
            :color (theme/color-secondary-contrast)
            :text-align :center}}
   [:h3
    "Sie wurden aufgerufen"]
   [:p "Bitte machen Sie sich auf den Weg zur Praxis"]])

(defn Ticket-Warten-Box []
  [:div
   {:style {
            :text-align :center}}
   [:h3
    "Sie sind eingecheckt"]
   [:p "Bitte warten Sie auf den Aufruf"]])

(defn Ticket-Call-To-Action-Box [ticket]
  (cond
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
       [muic/Data (notifications/info)]
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
  (let [ticket (get-in @(rf/subscribe [:app/db])
                       [:assets/asset-pools :wartsapp/ticket "myticket.edn"])]
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
     ;; [muic/Card [:div "Debug"] [muic/Data ticket]]])
     (when (or (-> ticket :aufgerufen)
               (not (-> ticket :eingecheckt)))
       [:> mui/Button
        {:variant :contained
         :color :primary
         :on-click #(rf/dispatch [:wartsapp/ticket-anfordern-clicked])}
        "Neues Ticket ziehen"])]))

(defn Ticket-Workarea []
  [Ticket])

(rf/reg-event-db
 :wartsapp/ticket-anfordern-clicked
 (fn [db _]
   (ajax/GET "/api/ziehe-ticket"
             {:handler (fn [response]
                         (let [ticket (reader/read-string response)]
                           (js/console.log "TICKET" ticket)
                           (rf/dispatch [:wartsapp/ticket-erhalten ticket])))
              :error-handler #(js/console.log "ERROR" %)})
   db))
   ;; (let [ticket-nummer (daten/neue-ticket-nummer)
   ;;       asset-path (str ticket-nummer ".edn")
   ;;       ticket (or (assets/asset db :wartsapp/tickets asset-path)
   ;;                  (daten/neues-ticket ticket-nummer "Anonymous"))]
   ;;   (-> db
   ;;       (assoc-in [:wartsapp :patient :ticket-nummer] ticket-nummer)
   ;;       (assets/set-asset :wartsapp/tickets asset-path ticket)))))


(rf/reg-event-db
 :wartsapp/poll-ticket
 (fn [db _]
   (when-let [ticket (get-in db [:assets/asset-pools :wartsapp/ticket "myticket.edn"])]
     (ajax/GET "/api/ticket"
               {:params {:id (-> ticket :id)}
                :handler (fn [response]
                           (let [ticket (reader/read-string response)]
                             (rf/dispatch [:wartsapp/ticket-erhalten ticket])))
                :error-handler #(js/console.log "ERROR" %)}))
   db))

(rf/reg-event-db
 :wartsapp/poll-schlange
 (fn [db _]
   (when-let [schlange (get-in db [:assets/asset-pools :wartsapp/schlange "myschlange.edn"])]
     (ajax/GET "/api/schlange"
               {:params {:id (-> schlange :id)}
                :handler (fn [response]
                           (let [schlange (reader/read-string response)]
                             (rf/dispatch [:wartsapp/schlange-erhalten schlange])))
                :error-handler #(js/console.log "ERROR" %)}))
   db))

(defn show-notification-wenn-aufgerufen [ticket]
  (when (-> ticket :aufgerufen)
    (let [localstorage-key (str "notification-aufgerufen-" (-> ticket :id))]
      (when-not (.getItem (.-localStorage js/window) localstorage-key)
        (notifications/show-notification
         "Sie wurden aufgerufen"
         {:body "Bitte machen Sie sich auf den Weg zur Praxis."
          :icon "/img/app-icon_128.png"
          :image "/img/app-icon_128.png"
          :lang "de"
          :tag (str "aufgerufen-" (-> ticket :id))
          :vibrate [300 200 100 200 300]
          :requireInteraction true})
        (.setItem (.-localStorage js/window) localstorage-key (daten/ts))))))

(rf/reg-event-db
 :wartsapp/ticket-erhalten
 (fn [db [_ ticket]]
   (show-notification-wenn-aufgerufen ticket)
   (assets/set-asset db :wartsapp/ticket "myticket.edn" ticket)))

(rf/reg-event-db
 :wartsapp/schlange-erhalten
 (fn [db [_ schlange]]
   (assets/set-asset db :wartsapp/schlange "myschlange.edn" schlange)))


(rf/reg-sub
 :wartsapp/ticket
 (fn [db _]
   (let [ticket (get-in db [:assets/asset-pools :wartsapp/ticket "myticket.edn"])]
     (js/console.log "ticket in sub:" ticket)
     ticket)))

(rf/reg-sub
 :wartsapp/schlange
 (fn [db _]
   (let [schlange (get-in db [:assets/asset-pools :wartsapp/schlange "myschlange.edn"])]
     (js/console.log "schlange in sub:" schlange)
     schlange)))


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
    " und "
    [:> mui/Link
     {:href "https://www.linkedin.com/in/artjom-weyand-74437a155/"
      :target :_blank}
     "Artjom Weyand"]
    " beim "]
   [:img
    {:src "/img/hackathon.jpg"
     :style {:max-width "100%"}}]
   [:p
    "Wenn Patienten zum Arzt gehen, kommt es in der Regel dazu, bis zur Behandlung im Wartezimmer Platz nehmen müssen. Hier ist das Ansteckungsrisiko jedoch besonders hoch."]
   [:p
    "Um dieses Risiko zu reduzieren, sollen die Patienten in einem digitalen Wartezimmer warten und sich dabei außerhalb der Praxis aufhalten, z.B. im eigenen Auto."]])


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
       {:href "ticket"}
       [:> mui/CardContent
        {:style {:text-align :center}}
        [:h3 "Ich bin Patient"]
        [:p "Ich möchte einen mobilen Warteplatz"]]]]
     [:> mui/Card
      [:> mui/CardActionArea
       {:href "schlange"}
       [:> mui/CardContent
        {:style {:text-align :center}}
        [:h3 "Ich bin Arzt"]
        [:p "Ich möchte ein mobiles Wartezimmer anbieten"]]]]]
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
     " | Frankenburg Softwaretechnik"]]])


(defn Desktop []
  [desktop/Desktop
   {:css (css)
    :app-bar [AppBar]
    :font-family "'Montserrat', sans-serif"
    :document-title-suffix "WartsApp"
    :footer [Footer]}])


(theme/set-theme! theme)
