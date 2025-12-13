## Dockerfile for single-module Spring Boot app

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Leverage layer caching
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy sources and build
COPY src ./src
RUN mvn -B -DskipTests package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install tools used by startup and healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget curl netcat-openbsd && \
    rm -rf /var/lib/apt/lists/*

# Optional: Cloud SQL Auth Proxy (legacy name kept as cloud_sql_proxy)
RUN wget -q https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O /cloud_sql_proxy && \
    chmod +x /cloud_sql_proxy && \
    mkdir -p /cloudsql

# Copy application JAR
COPY --from=build /workspace/target/*.jar /app/app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

# Expose app port
EXPOSE 8080

# Copy startup script from repo root
COPY startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

ENTRYPOINT ["/app/startup.sh"]
