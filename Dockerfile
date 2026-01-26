# Stage 1: Build the API
FROM docker.io/eclipse-temurin:21-jdk-alpine AS api-builder

WORKDIR /build

COPY api/build.gradle api/settings.gradle api/gradlew ./
COPY api/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY api/src ./src
RUN ./gradlew --no-daemon bootJar

# Stage 2: Build the UI
FROM docker.io/node:20-alpine AS ui-builder

WORKDIR /build

COPY ui/package*.json ./
RUN npm ci

COPY ui/ ./
RUN npm run build

# Stage 3: Runtime with supervisord
FROM docker.io/eclipse-temurin:21-jre-alpine

# Install required packages
RUN apk add --no-cache \
    curl \
    python3 \
    py3-bcrypt \
    nginx \
    supervisor

# Create directories
RUN mkdir -p /var/log/supervisor

# Copy built artifacts
COPY --from=api-builder /build/build/libs/flagd_admin_server-0.1.0.jar /app/api.jar
COPY --from=ui-builder  /build/dist /usr/share/nginx/html

# Copy configuration files
COPY docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY docker/nginx.conf /etc/nginx/http.d/default.conf
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh

# Create database file
RUN touch /app/app.db

# Make scripts executable
RUN chmod +x /usr/local/bin/entrypoint.sh

# Environment variables with secure defaults
ENV FLAGD_JWT_SECRET=""
ENV FLAGD_ADMIN_USERNAME=""
ENV FLAGD_ADMIN_PASSWORD_HASH=""
ENV FLAGD_AUTH_PROVIDER="jwt"
ENV FLAGD_ACCESS_TOKEN_EXPIRATION="900000"
ENV FLAGD_REFRESH_TOKEN_EXPIRATION="604800000"

# Set unified config with empty apiBaseUrl for nginx proxy
RUN echo '{"apiBaseUrl": ""}' > /usr/share/nginx/html/config.json

# Volumes
VOLUME ["/app"]

# Expose only the nginx port
EXPOSE 8080

# Health check for nginx (UI accessible)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
