# CQRS Read Model Implementation - Complete

## Overview
Successfully implemented read model projections to complete the CQRS (Command Query Responsibility Segregation) pattern in the VSA Payment Gateway microservices architecture.

## Problem Statement
The payment gateway had a complete write side (commands → aggregates → events) but the read side (queries) was not functioning because event projections weren't configured to update read models.

### Symptoms
- Customer registration succeeded (HTTP 201) ✓
- Events published to Kafka ✓
- Payment method addition failed with HTTP 404 ✗
- Cause: Read model tables empty, queries returned no data

## Solution Architecture

### CQRS Pattern Components

#### Write Side (Already Working)
```
POST /api/customers/register
  ↓
RegisterCustomerCommand
  ↓
CustomerAggregate (business logic)
  ↓
CustomerRegisteredEvent
  ↓
Axon Event Store (PostgreSQL)
  ↓
Kafka (payment-events topic)
```

#### Read Side (Implemented)
```
Kafka Events
  ↓
Tracking Event Processor
  ↓
CustomerProjection (@EventHandler)
  ↓
customer_read_model table
  ↓
CustomerQueryService
  ↓
GET /api/customers/{id}
```

## Implementation Details

### 1. Created AxonConfiguration.java
**Location:** `customer-service/src/main/java/com/vsa/paymentgateway/customer/config/AxonConfiguration.java`

**Purpose:** Configure Axon Framework event processing for read model projections

**Key Configuration:**

```java
@Configuration
@Profile("microservices")
public class AxonConfiguration {

    // XStream security configuration
    @Bean
    public XStream xStream() {
        XStream xStream = new XStream();
        xStream.allowTypesByWildcard(new String[]{"com.vsa.paymentgateway.**"});
        return xStream;
    }

    // Event processor configuration
    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer) {
        // Register tracking event processor
        configurer.registerTrackingEventProcessor(
            "CustomerProjection",
            org.axonframework.config.Configuration::eventStore,
            c -> TrackingEventProcessorConfiguration.forSingleThreadedProcessing()
        );
        
        // Assign projection handlers
        configurer.assignHandlerInstancesMatching(
            "CustomerProjection",
            eventHandler -> eventHandler.getClass().getName().contains("CustomerProjection")
        );
    }
}
```

**What it does:**
1. **XStream Security**: Allowlists VSA package classes for safe deserialization
2. **Tracking Processor**: Subscribes to event store and processes events sequentially
3. **Handler Assignment**: Routes events to CustomerProjection @EventHandler methods
4. **Token Management**: Tracks last processed event position for restart recovery

### 2. Made CustomerProjection Idempotent
**Location:** `customer-service/src/main/java/com/vsa/paymentgateway/customer/projection/CustomerProjection.java`

**Problem:** Event replay was causing duplicate key violations on email unique constraint

**Solution:** Added existence check before inserting

```java
@EventHandler
public void on(CustomerRegisteredEvent event) {
    // Idempotent: only create if doesn't exist
    if (!customerRepository.existsById(event.getCustomerId())) {
        CustomerReadModel customer = new CustomerReadModel(
            event.getCustomerId(),
            event.getCustomerName(),
            event.getEmail(),
            event.getPhoneNumber(),
            event.getAddress(),
            event.getTimestamp()
        );
        
        customerRepository.save(customer);
    }
}
```

**Benefits:**
- Safe event replay after system restart
- Handles duplicate events gracefully
- Allows token reset for debugging

### 3. Database Schema
**Tables Used:**

```sql
-- Event Store (write side)
domain_event_entry (
    global_index BIGSERIAL PRIMARY KEY,
    aggregate_identifier VARCHAR,
    sequence_number BIGINT,
    type VARCHAR,
    payload_type VARCHAR,
    payload BYTEA,
    time_stamp VARCHAR
)

-- Tracking Token (processor state)
token_entry (
    processor_name VARCHAR PRIMARY KEY,
    segment INTEGER,
    token BYTEA,
    timestamp VARCHAR,
    owner VARCHAR
)

-- Read Model (query side)
customer_read_model (
    customer_id VARCHAR PRIMARY KEY,
    customer_name VARCHAR,
    email VARCHAR UNIQUE,
    phone_number VARCHAR,
    address VARCHAR,
    created_at TIMESTAMP
)

payment_method_read_model (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR REFERENCES customer_read_model,
    masked_card_number VARCHAR,
    card_type VARCHAR,
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN,
    added_at TIMESTAMP
)
```

## Event Flow

### Customer Registration Flow
```
1. POST /api/customers/register
   → RegisterCustomerCommand sent to aggregate
   
2. CustomerAggregate validates and creates CustomerRegisteredEvent
   → Event stored in domain_event_entry (global_index++)
   
3. Event published to Kafka topic: payment-events
   → Other services can consume for cross-service communication
   
4. Tracking Event Processor (CustomerProjection)
   → Reads event from event store
   → Calls CustomerProjection.on(CustomerRegisteredEvent)
   → Inserts into customer_read_model table
   → Updates token_entry with new position
   
5. GET /api/customers/{id}
   → CustomerQueryService queries customer_read_model
   → Returns denormalized data optimized for reads
```

### Payment Method Addition Flow
```
1. POST /api/customers/{id}/payment-methods
   → Validates customer exists in READ MODEL (query side)
   → If found, sends AddPaymentMethodCommand
   
2. CustomerAggregate handles command
   → Validates business rules
   → Creates PaymentMethodAddedEvent
   
3. Event processed by CustomerProjection
   → Finds customer in read model
   → Adds payment method to customer.paymentMethods
   → Updates default flags if needed
   → Saves updated read model
```

## Troubleshooting Steps Performed

### Issue 1: StreamableKafkaMessageSource Bean Not Found
**Error:** 
```
Parameter 1 of method configureEventProcessing required a bean of type 
'StreamableKafkaMessageSource' that could not be found
```

**Solution:** Simplified configuration to use event store directly instead of Kafka message source
- Read models in same service consume from event store
- Kafka used for cross-service event distribution
- Cleaner separation of concerns

### Issue 2: Tracking Token at Wrong Position
**Symptom:** Token at position 19020, but latest event at 203
```
Token: GapAwareTrackingToken{index=19020}
Latest Event: global_index=203
```

**Solution:** Reset tracking token
```sql
DELETE FROM token_entry WHERE processor_name = 'CustomerProjection';
```
- Processor starts from beginning on restart
- Replays all events to rebuild read model
- Token automatically advances as events processed

### Issue 3: Duplicate Key Violation on Event Replay
**Error:**
```
PSQLException: duplicate key value violates unique constraint "uk_frn92owa2ackn3j7sck3sknvk"
Detail: Key (email)=(duplicate.test@example.com) already exists.
```

**Root Cause:** Projection not idempotent - always inserted without checking

**Solution 1:** Made projection idempotent with `existsById()` check

**Solution 2:** Cleared read model for clean rebuild
```sql
TRUNCATE TABLE customer_read_model CASCADE;
```

## Test Results

### Before Implementation
```
Test Duration: 17s
Tests Run: 3
Tests Passed: 2
Tests Failed: 1
Pass Rate: 66.7%

Failed Test:
✗ Payment Method Addition (HTTP 404 - Customer not found in read model)
```

### After Implementation
```
Test Duration: 48s
Tests Run: 5
Tests Passed: 4
Tests Failed: 1
Pass Rate: 80.0%

Passed Tests:
✓ Customer Registration
✓ Payment Method Addition  ← FIXED!
✓ Payment Authorization (AUTHORIZED)
✓ Payment Decline Handling

Remaining Issue:
⚠ Payment Event Flow (saga orchestration incomplete)
```

## Benefits Achieved

### 1. Complete CQRS Implementation
- **Write Side:** Optimized for commands and business logic validation
- **Read Side:** Optimized for queries with denormalized data
- **Separation:** Can scale independently based on load patterns

### 2. Eventual Consistency
- Commands complete immediately (low latency)
- Read models update asynchronously (acceptable delay)
- Tracking tokens ensure no events lost

### 3. Event Sourcing Integration
- Complete event history preserved
- Can rebuild read models from scratch
- Audit trail for all state changes

### 4. Scalability
- Read models can be optimized for specific queries
- Multiple read models from same events possible
- Event store handles concurrent writes

### 5. Demonstrable Pattern
Perfect showcase for:
- CQRS architecture
- Event Sourcing
- Eventual Consistency
- Event-Driven Microservices
- Domain Events

## Architecture Diagrams

### Component Diagram
```
┌─────────────────────────────────────────────────────────┐
│                    Customer Service                      │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────┐         ┌─────────────────┐          │
│  │   REST API   │         │  CustomerQuery  │          │
│  │  Controller  │         │     Service     │          │
│  └──────┬───────┘         └────────┬────────┘          │
│         │                          │                    │
│         │ Commands                 │ Queries            │
│         ▼                          ▼                    │
│  ┌──────────────┐         ┌─────────────────┐          │
│  │   Customer   │────────▶│ CustomerRead    │          │
│  │  Aggregate   │ Events  │ ModelRepository │          │
│  └──────┬───────┘         └─────────────────┘          │
│         │                          ▲                    │
│         │                          │                    │
│         ▼                          │                    │
│  ┌──────────────┐         ┌───────┴────────┐           │
│  │ Event Store  │────────▶│  Customer      │           │
│  │  (Postgres)  │ Stream  │  Projection    │           │
│  └──────┬───────┘         └────────────────┘           │
│         │                                                │
└─────────┼────────────────────────────────────────────────┘
          │
          ▼
    ┌─────────┐
    │  Kafka  │ ← Events published for other services
    └─────────┘
```

### Data Flow Diagram
```
Registration Request
       │
       ▼
┌─────────────┐
│   Command   │ POST /register
│   Handler   │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Aggregate  │ Business Logic
│ (Write Side)│ Validation
└──────┬──────┘
       │
       ▼
┌─────────────┐
│    Event    │ CustomerRegistered
│    Store    │ global_index: 204
└──────┬──────┘
       │
       ├────────────────┐
       │                │
       ▼                ▼
┌─────────────┐  ┌─────────────┐
│   Kafka     │  │  Tracking   │
│  Publisher  │  │  Processor  │
└─────────────┘  └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ Projection  │ @EventHandler
                 │   Handler   │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ Read Model  │ customer_read_model
                 │ (Query Side)│
                 └─────────────┘
                        │
                        ▼
                 GET /customers/{id}
```

## Next Steps

### 1. Replicate for Other Services
Apply same pattern to:
- Authorization Service (authorization read models)
- Processing Service (transaction read models)
- Settlement Service (settlement read models)

### 2. Optimize Projections
- Add database indexes on frequently queried fields
- Consider read-through caching (Redis)
- Implement projection versioning

### 3. Monitoring
- Track projection lag (time between event and read model update)
- Monitor token advancement rate
- Alert on projection errors/retries

### 4. Complete Saga Orchestration
- Fix remaining test failure (payment event flow)
- Verify end-to-end saga completion
- Add compensating transactions

## Configuration Files

### application.yml (customer-service)
```yaml
spring:
  profiles:
    active: microservices
    
  datasource:
    url: jdbc:postgresql://postgres:5432/payment_gateway
    username: postgres
    password: postgres
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

axon:
  axonserver:
    enabled: false
  kafka:
    client-id: customer-service
    default-topic: payment-events
    properties:
      bootstrap.servers: kafka:9092
      key.serializer: org.apache.kafka.common.serialization.StringSerializer
      value.serializer: org.axonframework.extensions.kafka.eventhandling.serialization.KafkaMessageSerializer
```

### Dockerfile
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/customer-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Verification Commands

### Check Read Model Data
```bash
docker exec vsa-postgres psql -U postgres -d payment_gateway \
  -c "SELECT customer_id, customer_name, email FROM customer_read_model LIMIT 10;"
```

### Check Tracking Token
```bash
docker exec vsa-postgres psql -U postgres -d payment_gateway \
  -c "SELECT processor_name, segment, token, timestamp FROM token_entry;"
```

### Check Event Store
```bash
docker exec vsa-postgres psql -U postgres -d payment_gateway \
  -c "SELECT global_index, aggregate_identifier, type FROM domain_event_entry ORDER BY global_index DESC LIMIT 10;"
```

### Monitor Projection Processing
```bash
docker-compose -f docker-compose-microservices.yml logs -f customer-service | grep -i "projection\|tracking"
```

## Conclusion

✅ **CQRS read model implementation complete**
✅ **Event projections working end-to-end**
✅ **Payment method addition now succeeds (80% test pass rate)**
✅ **Perfect showcase for event-driven architecture patterns**

The implementation demonstrates industry best practices for:
- Event Sourcing
- CQRS pattern
- Event-Driven Microservices
- Eventual Consistency
- Domain-Driven Design

**Status:** Production-ready for demo purposes
**Test Coverage:** 80% (4/5 scenarios passing)
**Next:** Complete saga orchestration to reach 100%
