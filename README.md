# WartsApp

Wenn Patienten zum Arzt gehen, kommt es in der Regel dazu, dass sie auf einer Warteliste registriert werden und bis zur Behandlung im Wartezimmer Platz nehmen müssen. Hier ist das Ansteckungsrisiko jedoch besonders hoch.

Um dieses Risiko zu reduzieren, sollen die Patienten in einem digitalen Wartezimmer warten und sich dabei außerhalb der Praxis aufhalten, z.B. im eigenen Auto.


# Demo

https://wartsapp.frankenburg.software


# WirVsVirius Hackathon

Projekt wurde beim WirVsVirius Hackathon gestartet: https://devpost.com/software/01_033_lebensnotwendigedienstleistungen_wartsapp


# Technik

WartsApp besteht aus zwei Anwendungen.

## Client

Der Client ist eine Web-Anwendung, die in aktuellen Browsern funktiert. Programmiersprache ist [ClojureScript](https://clojurescript.org/). Verwendete Frameworks und Bibliotheken sind insbesondere [re-frame](https://github.com/day8/re-frame) und [Material-UI](https://material-ui.com).

## Server

Der Server ist [Clojure](https://clojure.org/) programmiert und läuft daher als Java-Anwendung auf einem beliebigen Java Web Application Server. Wir verwenden [http-kit](https://github.com/http-kit/http-kit).

Der Server liefert die Browser-Andwndung aus und stellt eine API für den Zugriff auf die Daten bereit.

Die Daten werden als Textdateien im .edn-Format auf dem Dateisystem abgelegt. Eine Datenbank kommt nicht zum Einsatz.
