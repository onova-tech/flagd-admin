FROM node:18-alpine AS ui-builder

WORKDIR /ui

COPY ui/package*.json ./
RUN npm ci

COPY ui/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk-alpine AS api-builder

WORKDIR /api

COPY api/build.gradle api/settings.gradle ./
COPY api/gradle ./gradle
RUN ./gradlew --no-daemon dependencies

COPY api/ ./
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

COPY --from=api-builder /api/build/libs/flagd_admin_server-0.0.1-SNAPSHOT.jar /app/api.jar

WORKDIR /app

EXPOSE 9090

CMD ["java", "-jar", "/app/api.jar"]
