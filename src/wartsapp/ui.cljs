(ns wartsapp.ui
  (:require
     [clojure.string :as string]
     [goog.object :as gobj]
     ["@material-ui/core/colors" :as mui-colors]
     ["@material-ui/core" :as mui]
     ["@material-ui/icons" :as icons]
     [reagent.core :as r]

     [kcu.utils :as u]
     [kcu.bapp :as bapp]
     [mui-commons.components :as muic]
     [mui-commons.theme :as theme]
     [kcu.bapp-ui :as bapp-ui]
     [kunagi-base-browserapp.modules.desktop.components :as desktop]

     [wartsapp.appinfo :refer [appinfo]]
     [wartsapp.datenschutzerklaerung :as dse]))


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
        [:h3 "Ich bin Patientin"]
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
