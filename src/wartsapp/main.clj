(ns wartsapp.main
  (:require
   [kcu.sapp-init]
   [kcu.sapp :as sapp]

   [kcu.config :as config]
   [kunagi-base.modules.startup.api :as startup]

   ;; load kunagi-base modules
   [kunagi-base-server.modules.http-server.model]
   [kunagi-base-server.modules.browserapp.model]


   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.manager]))



(config/set-default-config!
 {:http-server/oauth {:google {:enabled? false}}})

(config/set-appinfo appinfo)


(defn -main []
  (startup/start!)
  (sapp/start))
