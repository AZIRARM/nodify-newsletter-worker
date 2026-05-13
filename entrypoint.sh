#!/bin/sh

# Démarrer nginx en arrière-plan
nginx

# Attendre que nginx soit prêt
sleep 2

# Démarrer l'application Spring Boot
java -jar app.jar