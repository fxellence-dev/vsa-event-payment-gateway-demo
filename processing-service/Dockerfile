# Multi-stage build for Processing Service
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy parent POM and common module first (for better caching)
COPY pom.xml .
COPY payment-gateway-common ./payment-gateway-common

# Copy all service modules (required for Maven reactor build)
COPY customer-service ./customer-service
COPY authorization-service ./authorization-service
COPY processing-service ./processing-service
COPY settlement-service ./settlement-service
COPY orchestration-service ./orchestration-service
COPY gateway-api ./gateway-api

# Build the application
RUN mvn clean package -pl processing-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built artifact from builder stage
COPY --from=builder /app/processing-service/target/*-exec.jar app.jar

# Expose the service port
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
