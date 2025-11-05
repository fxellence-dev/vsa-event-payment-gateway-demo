# VSA Payment Gateway - Microservices Deployment Guide

## Overview

This guide explains how to deploy the VSA Payment Gateway in **microservices mode**, where each service runs in its own Docker container and communicates via Kafka event bus.

---

## Architecture

### Microservices Deployment
```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                          │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  Customer    │  │ Authorization│  │  Processing  │    │
│  │   Service    │  │   Service    │  │   Service    │    │
│  │   :8081      │  │   :8082      │  │   :8083      │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
│         │                  │                  │            │
│  ┌──────▼───────┐  ┌──────▼───────┐                       │
│  │  Settlement  │  │Orchestration │                       │
│  │   Service    │  │   Service    │                       │
│  │   :8084      │  │   :8085      │                       │
│  └──────┬───────┘  └──────┬───────┘                       │
│         │                  │                               │
│  ┌──────▼──────────────────▼───────────────┐              │
│  │         Kafka Event Bus                  │              │
│  │         Topic: payment-events            │              │
│  │         Port: 9092                       │              │
│  └──────────────┬───────────────────────────┘              │
│                 │                                           │
│  ┌──────────────▼───────────────────────────┐              │
│  │         PostgreSQL Database              │              │
│  │         (Shared by all services)         │              │
│  │         Port: 5433                       │              │
│  └──────────────────────────────────────────┘              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

1. **Docker Desktop** installed and running
   - Download: https://www.docker.com/products/docker-desktop
   - Minimum version: 20.10+
   
2. **Docker Compose** installed
   - Usually included with Docker Desktop
   - Minimum version: 2.0+

3. **Available Ports** (make sure these are not in use):
   - 5433 (PostgreSQL)
   - 2181 (Zookeeper)
   - 9092 (Kafka)
   - 8081-8085 (Microservices)

4. **System Resources**:
   - Minimum: 8 GB RAM, 4 CPU cores
   - Recommended: 16 GB RAM, 8 CPU cores

---

## Quick Start

### 1. Start All Microservices

```bash
./start-microservices.sh
```

This script will:
- ✅ Build all Docker images (first time only, ~5-10 minutes)
- ✅ Start PostgreSQL, Zookeeper, Kafka
- ✅ Start all 5 microservices
- ✅ Wait for all services to be healthy
- ✅ Display service URLs and helpful commands

### 2. Check Service Status

```bash
docker-compose -f docker-compose-microservices.yml ps
```

All services should show status: **Up (healthy)**

### 3. View Logs

```bash
# View all logs
./logs-microservices.sh

# View specific service logs
./logs-microservices.sh customer-service
./logs-microservices.sh orchestration-service
```

### 4. Monitor Kafka Events

```bash
./monitor-kafka-events.sh
```

This will show all events flowing through the Kafka event bus in real-time.

### 5. Test the System

#### Register a Customer
```bash
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'
```

#### Add Payment Method
```bash
curl -X POST http://localhost:8081/api/customers/{customerId}/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }'
```

#### Authorize Payment
```bash
curl -X POST http://localhost:8082/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{customerId}",
    "amount": 100.00,
    "currency": "USD"
  }'
```

Watch the logs and Kafka monitor to see the saga flow:
1. **Authorization Service** → PaymentAuthorizedEvent
2. **Orchestration Service** → Sends ProcessPaymentCommand
3. **Processing Service** → PaymentProcessedEvent
4. **Orchestration Service** → Sends SettlePaymentCommand
5. **Settlement Service** → PaymentSettledEvent
6. **Orchestration Service** → Saga completes! ✓

### 6. Stop All Services

```bash
./stop-microservices.sh

# Or clean everything including data
./stop-microservices.sh --clean
```

---

## Service Details

### Infrastructure Services

| Service | Port | Description |
|---------|------|-------------|
| PostgreSQL | 5433 | Shared database for all services |
| Zookeeper | 2181 | Kafka coordination service |
| Kafka | 9092 | Event bus for inter-service communication |

### Microservices

| Service | Port | Health Check | Description |
|---------|------|--------------|-------------|
| Customer Service | 8081 | http://localhost:8081/actuator/health | Customer registration & payment methods |
| Authorization Service | 8082 | http://localhost:8082/actuator/health | Payment authorization & fraud detection |
| Processing Service | 8083 | http://localhost:8083/actuator/health | Payment processing (Stripe/Adyen simulation) |
| Settlement Service | 8084 | http://localhost:8084/actuator/health | Settlement processing (Bank/ACH simulation) |
| Orchestration Service | 8085 | http://localhost:8085/actuator/health | Saga orchestration for payment flow |

---

## Management Commands

### Docker Compose Commands

```bash
# Start all services
docker-compose -f docker-compose-microservices.yml up -d

# Stop all services
docker-compose -f docker-compose-microservices.yml down

# View logs
docker-compose -f docker-compose-microservices.yml logs -f

# View logs for specific service
docker-compose -f docker-compose-microservices.yml logs -f customer-service

# Restart a service
docker-compose -f docker-compose-microservices.yml restart customer-service

# Scale a service (run multiple instances)
docker-compose -f docker-compose-microservices.yml up -d --scale customer-service=3

# Check service status
docker-compose -f docker-compose-microservices.yml ps

# Remove everything including volumes
docker-compose -f docker-compose-microservices.yml down -v
```

### Individual Service Commands

```bash
# Execute command in a container
docker exec -it vsa-customer-service /bin/sh

# View container logs
docker logs -f vsa-customer-service

# Restart a container
docker restart vsa-customer-service

# Stop a container
docker stop vsa-customer-service

# Start a stopped container
docker start vsa-customer-service
```

### Kafka Management

```bash
# List topics
docker exec vsa-kafka kafka-topics --list --bootstrap-server localhost:9092

# Describe payment-events topic
docker exec vsa-kafka kafka-topics --describe --topic payment-events --bootstrap-server localhost:9092

# Create a new topic
docker exec vsa-kafka kafka-topics --create --topic my-topic --partitions 3 --replication-factor 1 --bootstrap-server localhost:9092

# Monitor events from beginning
docker exec -it vsa-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-events --from-beginning

# Monitor events from now
docker exec -it vsa-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-events
```

### Database Management

```bash
# Connect to PostgreSQL
docker exec -it vsa-postgres psql -U postgres -d payment_gateway

# Run SQL query
docker exec -it vsa-postgres psql -U postgres -d payment_gateway -c "SELECT * FROM customer_read_model;"

# Backup database
docker exec vsa-postgres pg_dump -U postgres payment_gateway > backup.sql

# Restore database
cat backup.sql | docker exec -i vsa-postgres psql -U postgres payment_gateway
```

---

## Configuration

### Environment Variables

All services can be configured via environment variables in `docker-compose-microservices.yml`:

```yaml
environment:
  # Spring Boot Profile
  SPRING_PROFILES_ACTIVE: microservices
  
  # Database Configuration
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_gateway
  SPRING_DATASOURCE_USERNAME: postgres
  SPRING_DATASOURCE_PASSWORD: postgres
  
  # Kafka Configuration
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  
  # Service Port
  SERVER_PORT: 8081
```

### Kafka Topics

| Topic | Description | Partitions | Replication Factor |
|-------|-------------|------------|-------------------|
| payment-events | All payment domain events | 1 | 1 |

**Events Published to payment-events:**
- CustomerRegisteredEvent
- PaymentMethodAddedEvent
- PaymentAuthorizedEvent
- PaymentAuthorizationDeclinedEvent
- AuthorizationVoidedEvent
- PaymentProcessedEvent
- PaymentProcessingFailedEvent
- PaymentRefundedEvent
- PaymentSettledEvent
- SettlementFailedEvent

---

## Troubleshooting

### Services Won't Start

**Problem**: Services fail to start or crash immediately

**Solutions**:
1. Check Docker is running: `docker info`
2. Check available disk space: `df -h`
3. Check available memory: `docker stats`
4. View service logs: `./logs-microservices.sh <service-name>`
5. Ensure ports are not in use: `lsof -i :8081` (Mac/Linux)

### Kafka Connection Issues

**Problem**: Services can't connect to Kafka

**Solutions**:
1. Wait for Kafka to be fully started (can take 30-60 seconds)
2. Check Kafka is healthy: `docker-compose -f docker-compose-microservices.yml ps kafka`
3. Check Kafka logs: `./logs-microservices.sh kafka`
4. Restart Kafka: `docker-compose -f docker-compose-microservices.yml restart kafka`

### Database Connection Issues

**Problem**: Services can't connect to PostgreSQL

**Solutions**:
1. Check PostgreSQL is healthy: `docker-compose -f docker-compose-microservices.yml ps postgres`
2. Check PostgreSQL logs: `./logs-microservices.sh postgres`
3. Test connection: `docker exec -it vsa-postgres psql -U postgres -c "SELECT 1;"`
4. Restart PostgreSQL: `docker-compose -f docker-compose-microservices.yml restart postgres`

### Saga Not Completing

**Problem**: Payment flow starts but doesn't complete

**Solutions**:
1. Check orchestration service logs: `./logs-microservices.sh orchestration-service`
2. Monitor Kafka events: `./monitor-kafka-events.sh`
3. Check all services are running: `docker-compose -f docker-compose-microservices.yml ps`
4. Verify event flow in each service's logs

### Out of Memory

**Problem**: Docker containers running out of memory

**Solutions**:
1. Increase Docker Desktop memory allocation (Settings → Resources)
2. Stop other Docker containers: `docker stop $(docker ps -q)`
3. Clean up unused images: `docker system prune -a`
4. Restart Docker Desktop

### Slow Performance

**Problem**: Services responding slowly

**Solutions**:
1. Check Docker resource usage: `docker stats`
2. Increase CPU allocation in Docker Desktop settings
3. Reduce number of running containers
4. Check database connection pool settings
5. Monitor Kafka consumer lag

---

## Performance Tuning

### Scale Services Horizontally

```bash
# Run 3 instances of customer service
docker-compose -f docker-compose-microservices.yml up -d --scale customer-service=3

# Run 2 instances of processing service
docker-compose -f docker-compose-microservices.yml up -d --scale processing-service=2
```

**Note**: Only scale stateless services. Do NOT scale orchestration-service (saga coordination requires single instance).

### Increase Kafka Partitions

```bash
# Increase partitions for better parallelism
docker exec vsa-kafka kafka-topics --alter --topic payment-events --partitions 3 --bootstrap-server localhost:9092
```

### Optimize Docker Resources

Edit `docker-compose-microservices.yml` to add resource limits:

```yaml
customer-service:
  # ... existing config
  deploy:
    resources:
      limits:
        cpus: '1.0'
        memory: 1G
      reservations:
        cpus: '0.5'
        memory: 512M
```

---

## Production Considerations

### For Production Deployment:

1. **Use External Database**
   - Replace embedded PostgreSQL with managed database (RDS, Cloud SQL, etc.)
   - Configure connection pooling
   - Enable SSL/TLS for database connections

2. **Use Managed Kafka**
   - Replace embedded Kafka with managed service (Confluent Cloud, Amazon MSK, etc.)
   - Configure multiple brokers for high availability
   - Enable topic replication (factor 3)
   - Increase partitions for scalability

3. **Add Service Discovery**
   - Use Kubernetes service discovery
   - Or Consul/Eureka for VM deployments

4. **Implement API Gateway**
   - Add gateway-api service as entry point
   - Implement rate limiting
   - Add authentication/authorization

5. **Add Monitoring**
   - Prometheus for metrics collection
   - Grafana for dashboards
   - ELK stack for log aggregation
   - Jaeger/Zipkin for distributed tracing

6. **Enable TLS/SSL**
   - Use certificates for all inter-service communication
   - Enable Kafka SSL
   - Enable database SSL

7. **Implement Circuit Breakers**
   - Use Resilience4j for fault tolerance
   - Configure retry policies
   - Add fallback mechanisms

8. **Secret Management**
   - Use Vault, AWS Secrets Manager, or similar
   - Don't hardcode credentials in docker-compose.yml

---

## Next Steps

- ✅ **You are here**: Microservices running in Docker
- ⏸️ Next: Configure deployment profiles (monolith vs microservices)
- ⏸️ Next: Update build configuration with Maven profiles
- ⏸️ Next: Comprehensive documentation updates
- ⏸️ Next: End-to-end testing of both deployment modes

---

**Last Updated**: 4 November 2025
**Version**: 1.0.0
