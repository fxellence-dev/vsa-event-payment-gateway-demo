# Phase 6 Complete: Distributed Event Bus (Kafka) Configuration ✅

## 🎉 Status: COMPLETED

Phase 6 of the Production Evolution Plan has been successfully completed!

---

## What Was Created

### 1. Maven Dependencies Added (5 POMs)

Added **Axon Kafka Spring Boot Starter** to all microservices:

```xml
<dependency>
    <groupId>org.axonframework.extensions.kafka</groupId>
    <artifactId>axon-kafka-spring-boot-starter</artifactId>
    <version>4.8.0</version>
</dependency>
```

**Files Updated:**
- ✅ `customer-service/pom.xml`
- ✅ `authorization-service/pom.xml`
- ✅ `processing-service/pom.xml`
- ✅ `settlement-service/pom.xml`
- ✅ `orchestration-service/pom.xml`

---

### 2. Kafka Configuration Classes (5 files)

Created `KafkaEventBusConfig.java` for each service using Spring Boot auto-configuration:

#### Customer Service
**File**: `customer-service/src/main/java/com/vsa/paymentgateway/customer/config/KafkaEventBusConfig.java`
- **Profile**: `@Profile("microservices")` - Only active in microservices mode
- **Purpose**: Enable Kafka event publishing for customer events
- **Events Published**: CustomerRegisteredEvent, PaymentMethodAddedEvent
- **Auto-Configuration**: Relies on axon-kafka-spring-boot-starter

#### Authorization Service
**File**: `authorization-service/src/main/java/com/vsa/paymentgateway/authorization/config/KafkaEventBusConfig.java`
- **Profile**: `@Profile("microservices")`
- **Purpose**: Enable Kafka event publishing for authorization events
- **Events Published**: PaymentAuthorizedEvent, PaymentAuthorizationDeclinedEvent, AuthorizationVoidedEvent
- **Critical Event**: PaymentAuthorizedEvent starts the PaymentProcessingSaga

#### Processing Service
**File**: `processing-service/src/main/java/com/vsa/paymentgateway/processing/config/KafkaEventBusConfig.java`
- **Profile**: `@Profile("microservices")`
- **Purpose**: Enable Kafka event publishing for processing events
- **Events Published**: PaymentProcessedEvent, PaymentProcessingFailedEvent, PaymentRefundedEvent
- **Success Rate**: 90% (simulated)

#### Settlement Service
**File**: `settlement-service/src/main/java/com/vsa/paymentgateway/settlement/config/KafkaEventBusConfig.java`
- **Profile**: `@Profile("microservices")`
- **Purpose**: Enable Kafka event publishing for settlement events
- **Events Published**: PaymentSettledEvent, SettlementFailedEvent
- **Success Rate**: 95% (simulated)

#### Orchestration Service
**File**: `orchestration-service/src/main/java/com/vsa/paymentgateway/orchestration/config/KafkaEventBusConfig.java`
- **Profile**: `@Profile("microservices")`
- **Purpose**: Enable Kafka for both consuming events and publishing commands
- **Commands Published**: ProcessPaymentCommand, SettlePaymentCommand, VoidAuthorizationCommand
- **Events Consumed**: All payment saga events from other services

---

## Kafka Event Flow Architecture

### Event Publishing Flow
```
┌─────────────────────────────────────────────────────────────┐
│                     Kafka Event Bus                         │
│                  Topic: payment-events                      │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │
    ┌─────────────────────────┼─────────────────────────┐
    │                         │                         │
    │                         │                         │
┌───▼────┐             ┌──────▼───┐              ┌─────▼─────┐
│Customer│             │   Auth   │              │Processing │
│Service │             │ Service  │              │  Service  │
│ :8081  │             │  :8082   │              │   :8083   │
└────────┘             └──────────┘              └───────────┘
    │                         │                         │
    ├─ CustomerRegistered     ├─ PaymentAuthorized     ├─ PaymentProcessed
    └─ PaymentMethodAdded     ├─ AuthorizationDeclined └─ PaymentProcessingFailed
                              └─ AuthorizationVoided
    
┌─────────────┐              ┌────────────────┐
│ Settlement  │              │ Orchestration  │
│   Service   │              │    Service     │
│   :8084     │              │     :8085      │
└─────────────┘              └────────────────┘
    │                                │
    ├─ PaymentSettled                ├─ Consumes ALL Events
    └─ SettlementFailed              └─ Publishes Commands
```

### Saga Orchestration via Kafka
```
PaymentProcessingSaga (Orchestration Service):

┌────────────────────────────────────────────────────────────┐
│ 1. START: PaymentAuthorizedEvent (from Kafka)             │
│    ↓                                                        │
│ 2. PUBLISH: ProcessPaymentCommand → Kafka                 │
│    ↓                                                        │
│ 3. CONSUME: PaymentProcessedEvent ← Kafka                 │
│    ↓                                                        │
│ 4. PUBLISH: SettlePaymentCommand → Kafka                  │
│    ↓                                                        │
│ 5. CONSUME: PaymentSettledEvent ← Kafka                   │
│    ↓                                                        │
│ 6. END: Saga Complete ✓                                    │
└────────────────────────────────────────────────────────────┘

COMPENSATION FLOW:
┌────────────────────────────────────────────────────────────┐
│ • PaymentProcessingFailedEvent ← Kafka                     │
│   → PUBLISH: VoidAuthorizationCommand → Kafka             │
│                                                             │
│ • SettlementFailedEvent ← Kafka                            │
│   → PUBLISH: VoidAuthorizationCommand → Kafka             │
│                                                             │
│ • AuthorizationVoidedEvent ← Kafka                         │
│   → END: Saga (compensated)                                │
└────────────────────────────────────────────────────────────┘
```

---

## Configuration Details

### Kafka Producer Configuration (All Services)

Each service has Kafka producer configuration in `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.ByteArraySerializer
      acks: all                    # Wait for all replicas
      retries: 3                    # Retry failed sends
```

### Kafka Consumer Configuration (All Services)

```yaml
spring:
  kafka:
    consumer:
      group-id: <service-name>-group    # e.g., customer-service-group
      auto-offset-reset: earliest       # Start from beginning if no offset
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.ByteArrayDeserializer
```

### Axon Kafka Configuration

```yaml
axon:
  kafka:
    client-id: <service-name>          # e.g., customer-service
    default-topic: payment-events      # Shared topic for all events
    publisher:
      confirmation-mode: transactional # Reliable publishing
```

---

## Consumer Group Strategy

| Service | Consumer Group | Purpose |
|---------|----------------|---------|
| Customer Service | `customer-service-group` | Consume own events for projections |
| Authorization Service | `authorization-service-group` | Consume commands and update authorizations |
| Processing Service | `processing-service-group` | Consume ProcessPaymentCommand |
| Settlement Service | `settlement-service-group` | Consume SettlePaymentCommand |
| Orchestration Service | `orchestration-service-group` | Consume all saga events (single instance) |

**Important**: Each consumer group ensures that:
- Events are processed once per service instance
- Saga instances don't duplicate (orchestration service)
- Scalability: Can run multiple instances of each service (except orchestration)

---

## Topic Strategy

### Shared Topic Approach (Implemented)

**Topic**: `payment-events` (all services publish to same topic)

**Advantages:**
- ✅ Simpler configuration
- ✅ Easier event ordering across services
- ✅ Single Kafka topic to manage
- ✅ All events in chronological order

**Consumer Groups Filter Events:**
- Each service's consumer group filters events by type
- Axon Framework handles event routing automatically
- Services only process events they're interested in

### Alternative: Topic-Per-Service (Not Implemented)

Could have used separate topics:
- `customer-events`
- `authorization-events`
- `processing-events`
- `settlement-events`
- `orchestration-commands`

**Trade-offs:**
- More granular control
- More complex configuration
- Better isolation
- More topics to manage

**Decision**: Shared topic is sufficient for this demo.

---

## Auto-Configuration Strategy

### Why Empty Configuration Classes?

The `KafkaEventBusConfig` classes are intentionally minimal because:

1. **Axon Kafka Spring Boot Starter** provides auto-configuration
2. All configuration is in `application.yml` (spring.kafka.* and axon.kafka.*)
3. No custom beans needed for basic Kafka integration
4. `@Profile("microservices")` ensures configuration only loads in microservices mode

### What Auto-Configuration Provides:

The `axon-kafka-spring-boot-starter` automatically creates:
- ✅ `KafkaPublisher` - Publishes Axon events to Kafka
- ✅ `KafkaMessageSource` - Consumes events from Kafka
- ✅ `KafkaMessageConverter` - Serializes/deserializes messages
- ✅ `ProducerFactory` - Creates Kafka producers
- ✅ `ConsumerFactory` - Creates Kafka consumers
- ✅ `EventProcessingConfigurer` - Configures event processors

### When You'd Need Custom Beans:

Only customize if you need:
- Custom topic routing logic
- Special error handling
- Custom serializers (non-XStream)
- Advanced Kafka features (transactions, idempotence)

For this demo, auto-configuration is perfect! ✓

---

## Deployment Modes

### Monolith Mode (Default)

```yaml
spring:
  profiles:
    active: default  # or omit this line
```

**Behavior:**
- ✅ `KafkaEventBusConfig` classes are **INACTIVE** (not loaded)
- ✅ Axon uses **in-memory event bus** (SimpleEventBus)
- ✅ All services run in single JVM (gateway-api.jar)
- ✅ No Kafka dependency required at runtime
- ✅ Events propagate instantly in-memory

**Use Case**: Development, small deployments, simpler operations

---

### Microservices Mode (New!)

```yaml
spring:
  profiles:
    active: microservices
```

**Behavior:**
- ✅ `KafkaEventBusConfig` classes are **ACTIVE** (loaded)
- ✅ Axon uses **Kafka event bus** (KafkaPublisher/MessageSource)
- ✅ Each service runs in separate JVM/container
- ✅ Kafka required at runtime (localhost:9092 or configured)
- ✅ Events propagate via Kafka topics

**Use Case**: Production, horizontal scaling, fault isolation

---

## Environment Variables

### Required for Microservices Mode

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=microservices

# Kafka Bootstrap Servers
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Database (shared across all services)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/payment_gateway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Optional: Override service port
SERVER_PORT=8081  # (or 8082, 8083, 8084, 8085)
```

### For Docker Deployment

```bash
# Kafka points to Docker network
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Database points to Docker container
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payment_gateway
```

---

## Testing Kafka Integration

### 1. Start Infrastructure

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Start Kafka + Zookeeper
docker-compose up -d zookeeper kafka
```

### 2. Wait for Kafka

```bash
# Wait for Kafka to be ready (~30 seconds)
sleep 30

# Verify Kafka is running
docker-compose ps
```

### 3. Run Services in Microservices Mode

```bash
# Terminal 1: Customer Service
cd customer-service
SPRING_PROFILES_ACTIVE=microservices mvn spring-boot:run

# Terminal 2: Authorization Service
cd authorization-service
SPRING_PROFILES_ACTIVE=microservices mvn spring-boot:run

# Terminal 3: Processing Service
cd processing-service
SPRING_PROFILES_ACTIVE=microservices mvn spring-boot:run

# Terminal 4: Settlement Service
cd settlement-service
SPRING_PROFILES_ACTIVE=microservices mvn spring-boot:run

# Terminal 5: Orchestration Service
cd orchestration-service
SPRING_PROFILES_ACTIVE=microservices mvn spring-boot:run
```

### 4. Monitor Kafka Topic

```bash
# Watch events flowing through Kafka
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning
```

### 5. Trigger Payment Flow

```bash
# Register customer
curl -X POST http://localhost:8081/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'

# Add payment method
curl -X POST http://localhost:8081/api/customers/{customerId}/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }'

# Authorize payment
curl -X POST http://localhost:8082/api/payments/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "{customerId}",
    "amount": 100.00,
    "currency": "USD"
  }'

# Watch logs in all service terminals to see:
# - PaymentAuthorizedEvent published to Kafka
# - Saga receives event and sends ProcessPaymentCommand
# - Processing service receives command and publishes PaymentProcessedEvent
# - Saga receives event and sends SettlePaymentCommand
# - Settlement service receives command and publishes PaymentSettledEvent
# - Saga receives event and completes!
```

---

## Key Files Created

### Configuration Classes (5 files):
1. ✅ `customer-service/src/main/java/com/vsa/paymentgateway/customer/config/KafkaEventBusConfig.java`
2. ✅ `authorization-service/src/main/java/com/vsa/paymentgateway/authorization/config/KafkaEventBusConfig.java`
3. ✅ `processing-service/src/main/java/com/vsa/paymentgateway/processing/config/KafkaEventBusConfig.java`
4. ✅ `settlement-service/src/main/java/com/vsa/paymentgateway/settlement/config/KafkaEventBusConfig.java`
5. ✅ `orchestration-service/src/main/java/com/vsa/paymentgateway/orchestration/config/KafkaEventBusConfig.java`

### POM Files Updated (5 files):
1. ✅ `customer-service/pom.xml` - Added axon-kafka-spring-boot-starter:4.8.0
2. ✅ `authorization-service/pom.xml` - Added axon-kafka-spring-boot-starter:4.8.0
3. ✅ `processing-service/pom.xml` - Added axon-kafka-spring-boot-starter:4.8.0
4. ✅ `settlement-service/pom.xml` - Added axon-kafka-spring-boot-starter:4.8.0
5. ✅ `orchestration-service/pom.xml` - Added axon-kafka-spring-boot-starter:4.8.0

### Existing Configuration (Already Complete):
- ✅ `customer-service/src/main/resources/application.yml` - Kafka settings present
- ✅ `authorization-service/src/main/resources/application.yml` - Kafka settings present
- ✅ `processing-service/src/main/resources/application.yml` - Kafka settings present
- ✅ `settlement-service/src/main/resources/application.yml` - Kafka settings present
- ✅ `orchestration-service/src/main/resources/application.yml` - Kafka settings present

**Total Files Created/Updated**: 10

---

## Reliability & Fault Tolerance

### Event Publishing Reliability

**Configuration:**
```yaml
acks: all        # Wait for all replicas to acknowledge
retries: 3       # Retry failed sends 3 times
```

**Guarantees:**
- ✅ Events are not lost (durable storage in Kafka)
- ✅ At-least-once delivery semantics
- ✅ Events survive broker failures (if replication configured)

### Event Consumption Reliability

**Configuration:**
```yaml
auto-offset-reset: earliest    # Start from beginning if no offset
group-id: <service>-group      # Track consumed offsets
```

**Guarantees:**
- ✅ Services don't miss events
- ✅ Can replay events from beginning
- ✅ Offset tracking prevents duplicate processing

### Saga Reliability

**Orchestration Service:**
- ✅ Single consumer group ensures one saga instance
- ✅ Saga state persisted in database
- ✅ Can recover from failures and resume
- ✅ Timeout protection (5-minute deadline)
- ✅ Compensation flows for failures

---

## Performance Considerations

### Topic Partitions

**Current**: Using default partitions (1)

**For Production**: Increase partitions for parallelism
```bash
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --partitions 3 \
  --replication-factor 2
```

### Consumer Concurrency

**Current**: Single-threaded event processing per service

**For Production**: Can configure parallel event processors
```yaml
axon:
  eventhandling:
    processors:
      <processor-name>:
        mode: tracking
        thread-count: 4
```

### Scaling

**Horizontal Scaling**: Can run multiple instances of:
- ✅ Customer Service
- ✅ Authorization Service
- ✅ Processing Service
- ✅ Settlement Service
- ⚠️ Orchestration Service (only 1 instance recommended for saga consistency)

---

## Progress Tracking

| Phase | Status | Completion |
|-------|--------|------------|
| Phase 1: Processing Service | ✅ COMPLETE | 100% |
| Phase 2: Settlement Service | ✅ COMPLETE | 100% |
| Phase 3: Saga Orchestration | ✅ COMPLETE | 100% |
| Phase 4: Microservices Structure | ✅ COMPLETE | 100% |
| Phase 5: Service Configuration | ✅ COMPLETE | 100% |
| **Phase 6: Kafka Event Bus** | **✅ COMPLETE** | **100%** |
| Phase 7: Docker Compose | ⏸️ PENDING | 0% |
| Phase 8: Deployment Profiles | ⏸️ PENDING | 0% |
| Phase 9: Build Configuration | ⏸️ PENDING | 0% |
| Phase 10: Documentation | ⏸️ PENDING | 0% |
| Phase 11: Testing | ⏸️ PENDING | 0% |

**Overall Progress**: 55% (6 of 11 phases complete)

---

## 🎯 Achievement Unlocked!

**Milestone**: Distributed Event Bus Configuration Complete

You now have:
- ✅ Axon Kafka extension added to all services
- ✅ Kafka configuration classes created (with auto-config)
- ✅ Producer/consumer configuration in application.yml
- ✅ Profile-based activation (microservices vs monolith)
- ✅ Shared topic strategy (payment-events)
- ✅ Consumer groups for each service
- ✅ Comprehensive documentation in code

**Next Decision Point**: 
1. Continue with Phase 7 (Docker Compose for microservices deployment)
2. Or pause to test Kafka integration manually
3. Or review the configuration

---

**Last Updated**: 4 November 2025
**Phase 6 Status**: ✅ COMPLETE
