;; Generated by kunagi-toolbox.

(ns wartsapp.appinfo)

(def
 appinfo
 {:browserapp {:colors {:background "#ffa726", :primary "#00695c"},
               :js-optimizations :simple,
               :notifications? true,
               :related-apps {:play "de.koczewski.android.rinteln"},
               :serviceworker {:cache-first [],
                               :no-cache [],
                               :pre-cache [],
                               :version 1},
               :standalone? true},
  :build-time "2020-03-22 12:38",
  :legal {:datenschutzerklaerung-bausteine [:vertragsabwicklung
                                            :serverdaten
                                            :cookies
                                            :kontaktanfragen
                                            :nutzerbeitraege],
          :vendor "Team Kunagi\nWitoslaw Koczewski, Unter der Frankenburg 20, 31737 Rinteln, wi@koczewski.de\nFabian Hager, mail@fabianhager.de\nArtjom Weyand, artjom.kochtchi@googlemail.com"},
  :project {:group "kunagi", :id "wartsapp", :name "WartsApp"},
  :release {:major 1, :minor 6},
  :serverapp {:config-dir "/etc/wartsapp",
              :executable-name "wartsappd",
              :executable-path "/usr/local/bin/wartsappd",
              :http-port 23009,
              :uri "https://wartsapp.frankenburg.software",
              :user-name "wartsapp",
              :vhost "wartsapp.frankenburg.software",
              :working-dir "/var/local/wartsapp"}})
