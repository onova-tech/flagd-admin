FROM docker.io/eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY . .
RUN ./gradlew --no-daemon bootJar

FROM docker.io/eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

COPY --from=builder /build/build/libs/flagd_admin_server-0.0.1-SNAPSHOT.jar /app/api.jar

RUN touch /app/app.db

WORKDIR /app

VOLUME ["/app"]

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

CMD ["java", "-jar", "/app/api.jar"]
