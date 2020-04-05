(ns wartsapp.fonts
  (:require
   [kcu.utils :as u]
   [kcu.butils :as bu]))


;; https://google-webfonts-helper.herokuapp.com/fonts/montserrat?subsets=latin

(def font-montserrat "
/* montserrat-regular - latin */
@font-face {
  font-family: 'Montserrat';
  font-style: normal;
  font-weight: 400;
  src: local('Montserrat Regular'), local('Montserrat-Regular'),
       url('../fonts/montserrat-v14-latin-regular.woff2') format('woff2'), /* Chrome 26+, Opera 23+, Firefox 39+ */
       url('../fonts/montserrat-v14-latin-regular.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
}

/* montserrat-500 - latin */
@font-face {
  font-family: 'Montserrat';
  font-style: normal;
  font-weight: 500;
  src: local('Montserrat Medium'), local('Montserrat-Medium'),
       url('../fonts/montserrat-v14-latin-500.woff2') format('woff2'), /* Chrome 26+, Opera 23+, Firefox 39+ */
       url('../fonts/montserrat-v14-latin-500.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
}
")

(def font-indieflower "
/* indie-flower-regular - latin */
@font-face {
  font-family: 'Indie Flower';
  font-style: normal;
  font-weight: 400;
  src: local('Indie Flower'), local('IndieFlower'),
       url('../fonts/indie-flower-v11-latin-regular.woff2') format('woff2'), /* Chrome 26+, Opera 23+, Firefox 39+ */
       url('../fonts/indie-flower-v11-latin-regular.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
}
")


(u/do-once
  (bu/install-css font-montserrat))
  ;(init/install-css font-indieflower))
