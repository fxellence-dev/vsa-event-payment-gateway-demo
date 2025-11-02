# Multi-stage build for Java 21 Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./
COPY payment-gateway-common/pom.xml ./payment-gateway-common/
COPY customer-service/pom.xml ./customer-service/
COPY authorization-service/pom.xml ./authorization-service/
COPY processing-service/pom.xml ./processing-service/
COPY settlement-service/pom.xml ./settlement-service/
COPY orchestration-service/pom.xml ./orchestration-service/
COPY gateway-api/pom.xml ./gateway-api/

# Download dependencies (this layer will be cached)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY payment-gateway-common/src ./payment-gateway-common/src
COPY customer-service/src ./customer-service/src
COPY authorization-service/src ./authorization-service/src
COPY processing-service/src ./processing-service/src
COPY settlement-service/src ./settlement-service/src
COPY orchestration-service/src ./orchestration-service/src
COPY gateway-api/src ./gateway-api/src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -g 1000 paymentgateway && \
    adduser -D -s /bin/sh -u 1000 -G paymentgateway paymentgateway

# Copy the built application
COPY --from=builder /app/gateway-api/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && chown -R paymentgateway:paymentgateway /app

# Switch to non-root user
USER paymentgateway

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM optimization for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]