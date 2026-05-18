FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache nginx

WORKDIR /app

COPY --from=build /app/target/newsletter-*.jar app.jar

COPY nginx.conf /etc/nginx/nginx.conf

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN mkdir -p /var/log/nginx && \
    mkdir -p /var/www/html && \
    mkdir -p /app/data

EXPOSE 80 8080