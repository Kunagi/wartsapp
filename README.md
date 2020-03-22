# WartsApp

Wenn Patienten zum Arzt gehen, kommt es in der Regel dazu, dass sie auf einer Warteliste registriert werden und bis zur Behandlung im Wartezimmer Platz nehmen müssen. Hier ist das Ansteckungsrisiko jedoch besonders hoch.

Um dieses Risiko zu reduzieren, sollen die Patienten in einem digitalen Wartezimmer warten und sich dabei außerhalb der Praxis aufhalten, z.B. im eigenen Auto.


# Demo

https://wartsapp.frankenburg.software


# WirVsVirius Hackathon

Projekt wurde beim WirVsVirius Hackathon gestartet: https://devpost.com/software/01_033_lebensnotwendigedienstleistungen_wartsapp


# Technik

WartsApp besteht aus zwei Anwendungen.

## Server

Der Server ist Clojure programmiert und läuft daher als Java-Anwendung auf einem beliebigen Java Web Application Server. Wir verwenden http-kit.
