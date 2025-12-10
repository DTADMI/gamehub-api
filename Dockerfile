# backend/Dockerfile (build from repository root with -f backend/Dockerfile .)

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy POMs first to benefit from Docker layer caching
COPY pom.xml ./
COPY backend/pom.xml backend/pom.xml

# Pre-fetch dependencies for offline build
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy sources and build only the backend module (and parents with -am)
COPY backend/src backend/src
RUN mvn -B -DskipTests -pl backend -am package

# Create a directory for the dependencies
RUN mkdir -p target/dependency
WORKDIR /workspace/backend
# Copy the dependencies
RUN mvn -B dependency:copy-dependencies -DoutputDirectory=target/dependency

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Install dependencies for Cloud SQL Proxy
RUN apt-get update && \
    apt-get install -y wget && \
    rm -rf /var/lib/apt/lists/*

# Download and install Cloud SQL Proxy
RUN wget -q https://dl.google.com/cloudsql/cloud_sql_proxy.linux.amd64 -O /cloud_sql_proxy && \
    chmod +x /cloud_sql_proxy && \
    mkdir -p /cloudsql

# Copy the application and its dependencies
COPY --from=build /workspace/backend/target/dependency/*.jar /app/lib/
COPY --from=build /workspace/backend/target/*.jar app.jar

# Set the classpath to include all JARs in the lib directory
ENV CLASSPATH=/app/lib/*:/app/app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose the port the app runs on
EXPOSE 8080

# Copy the startup script from the backend directory
COPY backend/startup.sh /app/startup.sh
RUN chmod +x /app/startup.sh

# Set the working directory
WORKDIR /app

# Use the startup script as the entrypoint
ENTRYPOINT ["/app/startup.sh"]
