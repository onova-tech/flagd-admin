FROM docker.io/eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY . .
RUN ./gradlew --no-daemon bootJar

FROM docker.io/eclipse-temurin:21-jre-alpine

# Install required utilities
RUN apk add --no-cache curl python3 py3-bcrypt

COPY --from=builder /build/build/libs/flagd_admin_server-0.1.1.jar /app/api.jar
COPY entrypoint.sh /usr/local/bin/

# Create database file
RUN touch /app/app.db

WORKDIR /app

# Environment variables with secure defaults
ENV FLAGD_JWT_SECRET=""
ENV FLAGD_ADMIN_USERNAME=""
ENV FLAGD_ADMIN_PASSWORD_HASH=""
ENV FLAGD_AUTH_PROVIDER="jwt"
ENV FLAGD_ACCESS_TOKEN_EXPIRATION="900000"
ENV FLAGD_REFRESH_TOKEN_EXPIRATION="604800000"

VOLUME ["/app"]

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["java", "-jar", "/app/api.jar"]
