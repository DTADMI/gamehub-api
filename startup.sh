#!/bin/bash
set -e

# Set default values for database connection
DB_HOST=localhost
DB_PORT=5432

# Start Cloud SQL Proxy if GCP_INSTANCE_CONNECTION_NAME is set
if [ -n "${GCP_INSTANCE_CONNECTION_NAME}" ]; then
  echo "Starting Cloud SQL Proxy for ${GCP_INSTANCE_CONNECTION_NAME}..."
  # Start Cloud SQL Proxy in the background
  /cloud_sql_proxy \
    -instances=${GCP_INSTANCE_CONNECTION_NAME}=tcp:${DB_PORT} \
    -ip_address_types=PRIVATE \
    -enable_iam_login \
    -structured_logs \
    -log_debug_stdout=true &
  
  # Wait for the proxy to start
  echo "Waiting for Cloud SQL Proxy to start..."
  sleep 5
  
  # Test the connection
  if ! nc -z ${DB_HOST} ${DB_PORT}; then
    echo "Failed to connect to Cloud SQL Proxy"
    exit 1
  fi
  echo "Cloud SQL Proxy started successfully"
fi

# Set the database URL for Spring Boot
if [ -n "${GCP_INSTANCE_CONNECTION_NAME}" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslmode=disable"
  echo "Using database URL: ${SPRING_DATASOURCE_URL}"
fi

# Start the Spring Boot application
echo "Starting Spring Boot application with profile: ${SPRING_PROFILES_ACTIVE:-default}"
exec java -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-default} "$@"
