(ns wartsapp.dev
  (:require
   [clojure.edn :as edn]

   [kunagi-base.logging.tap-formated]
   [kunagi-base.enable-asserts]

   [kcu.utils :as u]
   [kcu.txa :as txa]
   [kcu.query :as query]

   [wartsapp.manager]))


(tap> [:!!! ::!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! (-> (u/current-time-millis) java.util.Date.)])


(txa/reg-durable-aggregator-txa
 :wartsapp.aggregat
 {:projectors-as-responders [:wartsapp.schlange
                             :wartsapp.ticket]})



(txa/trigger-command!
 :wartsapp.manager
 [:ziehe-ticket {}])


(txa/trigger-command!
 :wartsapp.manager
 [:unterwegse
  {:ticket ticket-id}])


 ;; [:eroeffne-schlange
 ;;  ;; {:id "schlange-1"}
 ;;  {:id (str "schlange-" (u/current-time-millis))}])
 ;; [:checke-ein
 ;;  {:schlange-id "schlange-1"
 ;;   :ticket-nummer "abc"}])

(query/query-sync
 [:wartsapp.aggregat/wartsapp.ticket {:id "ticket-1"}]
 {:dummy :context})
