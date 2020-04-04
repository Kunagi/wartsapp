(ns wartsapp.main
  (:require
   [clojure.edn :as edn]

   [kunagi-base.logging.tap-formated]
   [kunagi-base.enable-asserts]
   [kunagi-base.appconfig.load-as-server]

   [kunagi-base.appconfig.api :as appconfig]
   [kunagi-base.modules.startup.api :as startup]

   ;; load kunagi-base modules
   [kunagi-base-server.modules.http-server.model]
   [kunagi-base-server.modules.browserapp.model]

   [kcu.sapp :as sapp]

   [wartsapp.appinfo :refer [appinfo]]
   [wartsapp.manager]))



(appconfig/set-default-config!
 {:http-server/oauth {:google {:enabled? false}}})


(defn -main []
  (startup/start! {:app/info appinfo}))


