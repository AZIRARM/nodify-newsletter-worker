# Étape 1 : Build de l'application Spring Boot
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copier le pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Image finale avec Spring Boot et Nginx
FROM eclipse-temurin:21-jre-alpine

# Installation de nginx
RUN apk add --no-cache nginx

WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/newsletter-*.jar app.jar

# Copier la configuration nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Copier le script d'entrée
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Créer les dossiers nécessaires
RUN mkdir -p /var/log/nginx && \
    mkdir -p /var/www/html && \
    mkdir -p /app/data

# Exposer les ports
EXPOSE 80 8080

ENTRYPOINT ["/entrypoint.sh"]