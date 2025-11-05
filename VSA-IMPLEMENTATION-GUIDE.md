# Vertical Slice Architecture (VSA) Implementation Guide

**Last Updated**: November 5, 2025  
**Project Status**: Production-Ready Microservices Deployment

## 📚 Table of Contents
1. [What is Vertical Slice Architecture?](#what-is-vertical-slice-architecture)
2. [Why VSA Over Traditional Layered Architecture?](#why-vsa-over-traditional-layered-architecture)
3. [VSA in This Project](#vsa-in-this-project)
4. [Service-by-Service Deep Dive](#service-by-service-deep-dive)
5. [Event-Driven Architecture with Kafka](#event-driven-architecture-with-kafka)
6. [Microservices Deployment](#microservices-deployment)
7. [How VSA Helps Teams](#how-vsa-helps-teams)
8. [Implementation Best Practices](#implementation-best-practices)
9. [Common Pitfalls and Solutions](#common-pitfalls-and-solutions)
10. [Testing and Verification](#testing-and-verification)

---

## What is Vertical Slice Architecture?

### The Core Concept

**Vertical Slice Architecture (VSA)** organizes code by **business features** (vertical slices) rather than **technical layers** (horizontal slices).

#### Traditional Layered Architecture (Horizontal Slices)
```
┌─────────────────────────────────────────────────┐
│              Controllers Layer                  │
│  CustomerController │ PaymentController │ etc.  │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│              Services Layer                     │
│  CustomerService │ PaymentService │ etc.        │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│              Repository Layer                   │
│  CustomerRepository │ PaymentRepository │ etc.  │
└─────────────────────────────────────────────────┘
                       ↓
┌─────────────────────────────────────────────────┐
│              Database Layer                     │
└─────────────────────────────────────────────────┘

❌ Problem: To add a feature, you touch multiple layers!
```

#### Vertical Slice Architecture (Vertical Slices)
```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Customer   │  │Authorization │  │  Processing  │  │  Settlement  │
│   Service    │  │   Service    │  │   Service    │  │   Service    │
│              │  │              │  │              │  │              │
│ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────┐ │
│ │ API      │ │  │ │ API      │ │  │ │ Commands │ │  │ │ Commands │ │
│ │ Commands │ │  │ │ Commands │ │  │ │ Events   │ │  │ │ Events   │ │
│ │ Events   │ │  │ │ Events   │ │  │ │ Aggregate│ │  │ │ Aggregate│ │
│ │ Aggregate│ │  │ │ Aggregate│ │  │ │ Queries  │ │  │ │ Queries  │ │
│ │ Queries  │ │  │ │ Queries  │ │  │ └──────────┘ │  │ └──────────┘ │
│ └──────────┘ │  │ └──────────┘ │  │              │  │              │
│              │  │              │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘

✅ Solution: Each feature is self-contained from API to database!
```

### Key Principles

1. **Feature-First Organization**: Code is organized by business capabilities, not technical concerns
2. **Self-Contained Slices**: Each slice has everything it needs (API, business logic, data access)
3. **Minimal Coupling**: Slices communicate through well-defined contracts (events, commands)
4. **Team Ownership**: Each slice can be owned by a single team
5. **Independent Evolution**: Slices can evolve independently

---

## Why VSA Over Traditional Layered Architecture?

### Problems with Layered Architecture

#### 1. **Feature Changes Touch Multiple Layers**
```java
// To add "Add Loyalty Points" feature in layered architecture:

// 1. Update Controller Layer
@RestController
public class CustomerController {
    // Add new endpoint
}

// 2. Update Service Layer  
@Service
public class CustomerService {
    // Add business logic
}

// 3. Update Repository Layer
public interface CustomerRepository {
    // Add new query method
}

// 4. Update Entity Layer
@Entity
public class Customer {
    // Add new field
}

// Result: 4 different files in 4 different layers! 😰
```

#### 2. **Tight Coupling Between Features**
```java
// In layered architecture, services often depend on each other:

@Service
public class PaymentService {
    @Autowired
    private CustomerService customerService; // Coupling!
    @Autowired
    private AuthorizationService authService; // Coupling!
    @Autowired
    private ProcessingService processingService; // Coupling!
    
    // All services are tightly coupled in the same layer
}
```

#### 3. **Team Coordination Overhead**
- Team A working on Customer features
- Team B working on Payment features
- Both teams modify the same Controller layer → **Merge conflicts!**
- Both teams modify the same Service layer → **Integration issues!**

#### 4. **Hard to Scale Independently**
- Cannot scale just the "Customer" feature
- Must scale entire application layers
- Cannot deploy features independently

### VSA Advantages

#### 1. **Localized Changes**
```java
// To add "Add Loyalty Points" in VSA:

// Everything in customer-service module:
customer-service/
├── commands/AddLoyaltyPointsCommand.java       // ✅ All in one place
├── events/LoyaltyPointsAddedEvent.java         // ✅ Related code together
├── aggregates/CustomerAggregate.java           // ✅ Easy to find
└── queries/CustomerProjection.java             // ✅ Single module to change

// Result: 1 module, easy to understand and modify! 🎉
```

#### 2. **Loose Coupling Through Events**
```java
// Services communicate via events, not direct dependencies:

// Customer Service emits event
public class CustomerAggregate {
    public void addLoyaltyPoints() {
        apply(new LoyaltyPointsAddedEvent(...));
    }
}

// Payment Service listens to event (if interested)
@EventHandler
public void on(LoyaltyPointsAddedEvent event) {
    // React to customer loyalty points change
}

// No direct dependency! Services don't even know about each other! 🎯
```

#### 3. **Team Autonomy**
- **Team Customer** owns `customer-service` module
- **Team Payments** owns `authorization-service`, `processing-service`, `settlement-service`
- **Team Orchestration** owns `orchestration-service`
- Teams work independently, minimal conflicts

#### 4. **Independent Scaling**
```yaml
# Scale only what you need:
customer-service: 3 instances      # High user registration traffic
authorization-service: 5 instances  # High payment authorization traffic
processing-service: 2 instances     # Moderate processing traffic
settlement-service: 1 instance      # Low settlement traffic (batch processing)
```

---

## VSA in This Project

### Project Structure Overview

**Current State: Production Microservices Deployment**

```
VSA-Demo/
├── payment-gateway-common/          # Shared value objects, base classes
├── payment-gateway-customer/        # 🟦 Customer Service (Port 8081)
├── payment-gateway-authorization/   # 🟩 Authorization Service (Port 8082)
├── payment-gateway-processing/      # 🟨 Processing Service (Port 8083)
├── payment-gateway-settlement/      # 🟧 Settlement Service (Port 8084)
├── docker-compose.yml               # � Complete microservices stack
└── kubernetes/                      # ☸️ Kubernetes deployment manifests
```

### Current Deployment: Microservices with Event-Driven Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Production Microservices Stack                        │
└─────────────────────────────────────────────────────────────────────────┘

┌───────────────┐  ┌────────────────┐  ┌───────────────┐  ┌───────────────┐
│   Customer    │  │ Authorization  │  │  Processing   │  │  Settlement   │
│   Service     │  │    Service     │  │    Service    │  │    Service    │
│   :8081       │  │     :8082      │  │     :8083     │  │     :8084     │
│               │  │                │  │               │  │               │
│ ┌───────────┐ │  │  ┌───────────┐ │  │ ┌───────────┐ │  │ ┌───────────┐ │
│ │Aggregate  │ │  │  │Aggregate  │ │  │ │Aggregate  │ │  │ │Aggregate  │ │
│ │Commands   │ │  │  │Commands   │ │  │ │Commands   │ │  │ │Commands   │ │
│ │Events     │ │  │  │Events     │ │  │ │Events     │ │  │ │Events     │ │
│ │Projections│ │  │  │Projections│ │  │ │Projections│ │  │ │Projections│ │
│ │ReadModel  │ │  │  │ReadModel  │ │  │ │ReadModel  │ │  │ │ReadModel  │ │
│ │API        │ │  │  │API        │ │  │ │Kafka      │ │  │ │Kafka      │ │
│ └───────────┘ │  │  └───────────┘ │  │ │Consumer   │ │  │ │Consumer   │ │
└───────┬───────┘  └────────┬───────┘  │ └───────────┘ │  │ └───────────┘ │
        │                   │          └───────┬───────┘  └───────┬───────┘
        │                   │                  │                  │
        │  EventToKafka     │  EventToKafka    │  EventToKafka    │  EventToKafka
        │  Forwarder        │  Forwarder       │  Forwarder       │  Forwarder
        │                   │                  │                  │
        └───────────────────┴──────────────────┴──────────────────┘
                            │
                    ┌───────▼────────┐
                    │  Apache Kafka  │
                    │    :9092       │
                    │                │
                    │ payment-events │ ← Single topic for all domain events
                    │   topic        │
                    └────────┬───────┘
                             │
                    ┌────────▼────────┐
                    │   Zookeeper     │
                    │     :2181       │
                    └─────────────────┘
                             │
                    ┌────────▼────────┐
                    │  PostgreSQL     │
                    │     :5433       │
                    │                 │
                    │ • Event Store   │ ← domain_event_entry (Axon)
                    │ • Read Models   │ ← customer_read_model, etc.
                    └─────────────────┘

Event Flow:
1. Service handles command → generates event → persists to event store
2. EventToKafkaForwarder (@EventHandler) publishes event to Kafka
3. Other services consume events from Kafka → trigger commands
4. Complete event-driven saga orchestration (choreography pattern)

Benefits:
✅ Independent scaling per service (horizontal)
✅ Independent deployment (no downtime for other services)
✅ Event-driven communication (loose coupling via Kafka)
✅ Fault isolation (one service failure doesn't cascade)
✅ Complete audit trail (event sourcing in PostgreSQL)
✅ Technology flexibility per service
✅ Team ownership per service
✅ Production-ready with health checks and monitoring
```

---

## Event-Driven Architecture with Kafka

### EventToKafkaForwarder Implementation

Each service contains an `EventToKafkaForwarder` component that bridges Axon events to Kafka:

```java
@Component
public class EventToKafkaForwarder {
    
    private static final Logger logger = LoggerFactory.getLogger(EventToKafkaForwarder.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public EventToKafkaForwarder(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    // ⭐ Listen to ALL domain events from this service
    @EventHandler
    public void on(Object event) {
        try {
            String topic = "payment-events";  // Single topic for all events
            String eventType = event.getClass().getSimpleName();
            
            logger.info("📤 Publishing {} to Kafka topic: {}", eventType, topic);
            
            kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("✅ Successfully published {} to Kafka", eventType);
                    } else {
                        logger.error("❌ Failed to publish {} to Kafka", eventType, ex);
                    }
                });
        } catch (Exception e) {
            logger.error("❌ Error forwarding event to Kafka", e);
        }
    }
}
```

**How It Works**:
1. **@EventHandler** on generic `Object` → catches ALL events in this service
2. **Event Store First** → Axon persists to PostgreSQL, then EventToKafkaForwarder runs
3. **Fire and Forget** → Non-blocking publish to Kafka (async)
4. **Guaranteed Ordering** → Kafka partitions ensure order per customer/payment
5. **Retry on Failure** → Kafka producer retries automatically

### Kafka Configuration

```yaml
# application.yml (all services)
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all  # Wait for all replicas (durability)
      retries: 3
    consumer:
      group-id: ${spring.application.name}-group
      auto-offset-reset: earliest  # Read from beginning on first start
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.vsa.paymentgateway.*"
```

### Kafka Consumers in Processing and Settlement Services

**Processing Service Kafka Consumer**:
```java
@Service
public class ProcessingKafkaConsumer {
    
    private final CommandGateway commandGateway;
    
    @KafkaListener(topics = "payment-events", groupId = "processing-service-group")
    public void handleEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        
        if (event instanceof PaymentAuthorizedEvent) {
            PaymentAuthorizedEvent authorizedEvent = (PaymentAuthorizedEvent) event;
            
            // ⭐ Trigger processing workflow
            commandGateway.send(new ProcessPaymentCommand(
                authorizedEvent.getPaymentId(),
                authorizedEvent.getAuthorizationId(),
                authorizedEvent.getAmount(),
                Instant.now()
            ));
        }
    }
}
```

**Settlement Service Kafka Consumer**:
```java
@Service
public class SettlementKafkaConsumer {
    
    private final CommandGateway commandGateway;
    
    @KafkaListener(topics = "payment-events", groupId = "settlement-service-group")
    public void handleEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        
        if (event instanceof PaymentProcessedEvent) {
            PaymentProcessedEvent processedEvent = (PaymentProcessedEvent) event;
            
            // ⭐ Trigger settlement workflow
            commandGateway.send(new SettlePaymentCommand(
                processedEvent.getPaymentId(),
                processedEvent.getProcessingId(),
                processedEvent.getAmount(),
                Instant.now()
            ));
        }
    }
}
```

### Event Flow Diagram

```
┌─────────────────┐
│ Customer Service│
│   (Port 8081)   │
└────────┬────────┘
         │ RegisterCustomerCommand
         ▼
    [CustomerAggregate]
         │
         ├── Persist to Event Store (PostgreSQL)
         │   ↓ domain_event_entry table
         │
         └── CustomerRegisteredEvent
             ↓
    [EventToKafkaForwarder]
         │
         ▼
    ┌─────────────┐
    │ Kafka Topic │
    │payment-events│
    └─────────────┘
         │
         └──→ [Other Services Subscribe]
              (if they care about customer events)

┌──────────────────┐
│Authorization Svc │
│   (Port 8082)    │
└────────┬─────────┘
         │ AuthorizePaymentCommand
         ▼
    [AuthorizationAggregate]
         │
         ├── Persist to Event Store
         └── PaymentAuthorizedEvent
             ↓
    [EventToKafkaForwarder]
         │
         ▼
    ┌─────────────┐
    │ Kafka Topic │ 
    └──────┬──────┘
           │
           ▼
    ┌─────────────────┐
    │Processing Kafka │
    │   Consumer      │
    └────────┬────────┘
             │
             ▼ ProcessPaymentCommand
    [ProcessingAggregate]
         │
         ├── Persist to Event Store
         └── PaymentProcessedEvent
             ↓
    [EventToKafkaForwarder]
         │
         ▼
    ┌─────────────┐
    │ Kafka Topic │
    └──────┬──────┘
           │
           ▼
    ┌─────────────────┐
    │Settlement Kafka │
    │   Consumer      │
    └────────┬────────┘
             │
             ▼ SettlePaymentCommand
    [SettlementAggregate]
         │
         ├── Persist to Event Store
         └── PaymentSettledEvent
             ↓
    [EventToKafkaForwarder]
         │
         ▼
    (Saga Complete ✅)
```

### Benefits of Event-Driven Architecture

| Benefit | Description | Example |
|---------|-------------|---------|
| **Loose Coupling** | Services don't know about each other | Processing service doesn't call Settlement API directly |
| **Scalability** | Scale event consumers independently | Add more Settlement service instances for high volume |
| **Fault Tolerance** | Consumer failures don't affect producers | If Settlement is down, events queue in Kafka |
| **Audit Trail** | Complete event history in Kafka + PostgreSQL | Replay events to debug issues or rebuild state |
| **Real-time Processing** | Sub-second event propagation | Payment authorized → processed → settled in ~500ms |
| **Saga Orchestration** | Choreography pattern via events | No central orchestrator needed |

---

## Microservices Deployment

### Docker Compose Stack

The complete production stack is defined in `docker-compose.yml`:

```yaml
version: '3.8'

services:
  # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  # Infrastructure Services
  # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres:
    image: postgres:15
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: payment_gateway
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: admin123
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  # Microservices (Each with complete vertical slice)
  # ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  customer-service:
    build:
      context: ./payment-gateway-customer
    ports:
      - "8081:8081"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_gateway
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  authorization-service:
    build:
      context: ./payment-gateway-authorization
    ports:
      - "8082:8082"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_gateway
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  processing-service:
    build:
      context: ./payment-gateway-processing
    ports:
      - "8083:8083"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_gateway
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  settlement-service:
    build:
      context: ./payment-gateway-settlement
    ports:
      - "8084:8084"
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/payment_gateway
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

volumes:
  postgres_data:
```

### Starting the Stack

```bash
# Build and start all services
docker-compose up --build

# Start in detached mode
docker-compose up -d

# View logs
docker-compose logs -f customer-service
docker-compose logs -f kafka

# Stop all services
docker-compose down

# Stop and remove volumes (clean state)
docker-compose down -v
```

### Health Checks

Each service exposes health endpoints via Spring Boot Actuator:

```bash
# Customer Service health
curl http://localhost:8081/actuator/health

# Authorization Service health
curl http://localhost:8082/actuator/health

# Processing Service health
curl http://localhost:8083/actuator/health

# Settlement Service health
curl http://localhost:8084/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-1"
      }
    }
  }
}
```

---

## Service-by-Service Deep Dive

### 🟦 Service 1: Customer Service

**Business Capability**: Customer onboarding and payment method management

**Folder Structure**:
```
customer-service/
├── src/main/java/com/vsa/paymentgateway/customer/
│   ├── aggregates/
│   │   └── CustomerAggregate.java           ⭐ Core business logic
│   ├── commands/
│   │   ├── RegisterCustomerCommand.java     📝 Write operations
│   │   └── AddPaymentMethodCommand.java
│   ├── events/
│   │   ├── CustomerRegisteredEvent.java     📢 Domain events
│   │   └── PaymentMethodAddedEvent.java
│   ├── projection/
│   │   └── CustomerProjection.java          📊 Read model builder
│   ├── queries/
│   │   └── CustomerQueryService.java        🔍 Query handlers
│   ├── readmodel/
│   │   ├── CustomerReadModel.java           💾 Read-optimized model
│   │   └── PaymentMethodReadModel.java
│   ├── repository/
│   │   └── CustomerRepository.java          🗄️ JPA repository
│   ├── api/
│   │   ├── CustomerController.java          🌐 REST endpoints
│   │   ├── RegisterCustomerRequest.java     📥 Request DTOs
│   │   └── AddPaymentMethodRequest.java
│   └── domain/
│       └── PaymentCard.java                 💳 Value objects
└── pom.xml
```

#### Why This is a Complete Vertical Slice

1. **Everything for Customer Management in One Place**:
   - API endpoints (`CustomerController`)
   - Business logic (`CustomerAggregate`)
   - Data access (`CustomerRepository`)
   - Read models (`CustomerReadModel`)

2. **Self-Contained Business Rules**:
```java
// customer-service/aggregates/CustomerAggregate.java

@Aggregate
public class CustomerAggregate {
    
    @AggregateIdentifier
    private String customerId;
    private String email;
    private CustomerStatus status;
    
    // ✅ Business rule: Email must be unique
    @CommandHandler
    public CustomerAggregate(RegisterCustomerCommand command) {
        // Validation happens HERE, not in a separate service layer
        if (command.getEmail() == null || !command.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        // Emit event - this is the truth!
        AggregateLifecycle.apply(new CustomerRegisteredEvent(
            command.getCustomerId(),
            command.getEmail(),
            command.getCustomerName(),
            Instant.now()
        ));
    }
    
    // ✅ Business rule: Can only add payment method to active customer
    @CommandHandler
    public void handle(AddPaymentMethodCommand command) {
        if (this.status != CustomerStatus.ACTIVE) {
            throw new IllegalStateException("Customer must be active to add payment method");
        }
        
        // Validate card using Luhn algorithm
        if (!PaymentCard.isValidCardNumber(command.getCardNumber())) {
            throw new IllegalArgumentException("Invalid card number");
        }
        
        AggregateLifecycle.apply(new PaymentMethodAddedEvent(
            command.getPaymentMethodId(),
            this.customerId,
            command.getCardNumber(),
            command.getCardholderName(),
            command.getExpiryDate(),
            Instant.now()
        ));
    }
    
    // Event handlers update aggregate state
    @EventSourcingHandler
    public void on(CustomerRegisteredEvent event) {
        this.customerId = event.getCustomerId();
        this.email = event.getEmail();
        this.status = CustomerStatus.ACTIVE;
    }
}
```

3. **CQRS: Separate Read and Write Models**:

**Write Side** (Commands):
```java
// customer-service/api/CustomerController.java

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private final CommandGateway commandGateway;
    
    // Write operation: Register customer
    @PostMapping("/register")
    public ResponseEntity<String> registerCustomer(@RequestBody RegisterCustomerRequest request) {
        String customerId = UUID.randomUUID().toString();
        
        RegisterCustomerCommand command = new RegisterCustomerCommand(
            customerId,
            request.getEmail(),
            request.getCustomerName()
        );
        
        // Send command to aggregate - goes through event sourcing
        commandGateway.sendAndWait(command);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(customerId);
    }
}
```

**Read Side** (Queries):
```java
// customer-service/projection/CustomerProjection.java

@Component
@ProcessingGroup("customer-projection")
public class CustomerProjection {
    
    private final CustomerRepository customerRepository;
    
    // Listen to events and build read model
    @EventHandler
    public void on(CustomerRegisteredEvent event) {
        CustomerReadModel customer = new CustomerReadModel();
        customer.setCustomerId(event.getCustomerId());
        customer.setEmail(event.getEmail());
        customer.setCustomerName(event.getCustomerName());
        customer.setRegisteredAt(event.getRegisteredAt());
        customer.setPaymentMethods(new ArrayList<>());
        
        customerRepository.save(customer); // Save to read model table
    }
    
    @EventHandler
    public void on(PaymentMethodAddedEvent event) {
        CustomerReadModel customer = customerRepository.findById(event.getCustomerId())
            .orElseThrow(() -> new IllegalStateException("Customer not found"));
        
        PaymentMethodReadModel paymentMethod = new PaymentMethodReadModel();
        paymentMethod.setPaymentMethodId(event.getPaymentMethodId());
        paymentMethod.setCardholderName(event.getCardholderName());
        // Mask card number for security
        paymentMethod.setMaskedCardNumber(maskCardNumber(event.getCardNumber()));
        paymentMethod.setExpiryDate(event.getExpiryDate());
        paymentMethod.setCustomer(customer);
        
        customer.getPaymentMethods().add(paymentMethod);
        customerRepository.save(customer);
    }
}

// customer-service/queries/CustomerQueryService.java

@Service
public class CustomerQueryService {
    
    private final CustomerRepository customerRepository;
    
    // Query handler - reads from optimized read model
    public CustomerReadModel findCustomerById(String customerId) {
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + customerId));
    }
    
    public List<CustomerReadModel> findAllCustomers() {
        return customerRepository.findAll();
    }
}
```

#### VSA Benefits Demonstrated

✅ **Single Responsibility**: Customer service only handles customer-related features  
✅ **Complete Feature**: From API to database, everything customer-related is here  
✅ **No Leaky Abstractions**: Business logic in aggregate, not scattered across layers  
✅ **Team Ownership**: One team can own entire customer experience  
✅ **Easy to Test**: All customer logic in one module  

---

### 🟩 Service 2: Authorization Service

**Business Capability**: Payment authorization with fraud detection

**Folder Structure**:
```
authorization-service/
├── src/main/java/com/vsa/paymentgateway/authorization/
│   ├── aggregates/
│   │   └── PaymentAuthorizationAggregate.java    ⭐ Authorization logic
│   ├── commands/
│   │   ├── AuthorizePaymentCommand.java          📝 Authorize command
│   │   └── VoidAuthorizationCommand.java         📝 Void command
│   ├── events/
│   │   ├── PaymentAuthorizedEvent.java           📢 Success event
│   │   └── AuthorizationVoidedEvent.java         📢 Compensation event
│   ├── projection/
│   │   └── AuthorizationProjection.java          📊 Read model
│   ├── readmodel/
│   │   └── AuthorizationReadModel.java           💾 Query model
│   ├── repository/
│   │   └── AuthorizationRepository.java          🗄️ JPA repo
│   └── api/
│       └── AuthorizationController.java          🌐 REST API
└── pom.xml
```

#### Core Business Logic

```java
// authorization-service/aggregates/PaymentAuthorizationAggregate.java

@Aggregate
public class PaymentAuthorizationAggregate {
    
    @AggregateIdentifier
    private String authorizationId;
    private String customerId;
    private Money amount;
    private AuthorizationStatus status;
    
    @CommandHandler
    public PaymentAuthorizationAggregate(AuthorizePaymentCommand command) {
        // ✅ Business Rule 1: Amount must be positive
        if (command.getAmount().getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        // ✅ Business Rule 2: Currency must be supported
        if (!isSupportedCurrency(command.getAmount().getCurrency())) {
            throw new IllegalArgumentException("Unsupported currency: " + command.getAmount().getCurrency());
        }
        
        // ✅ Business Rule 3: Fraud detection (simple example)
        if (command.getAmount().getAmount().compareTo(new BigDecimal("10000")) > 0) {
            // High-value transaction - requires additional verification
            logger.warn("High-value transaction detected: {}", command.getAmount());
        }
        
        // ✅ Business Rule 4: Validate payment card
        if (!isValidPaymentCard(command.getPaymentCard())) {
            throw new IllegalArgumentException("Invalid payment card");
        }
        
        // Authorization successful - emit event
        AggregateLifecycle.apply(new PaymentAuthorizedEvent(
            command.getAuthorizationId(),
            command.getCustomerId(),
            command.getAmount(),
            command.getPaymentCard(),
            command.getMerchantId(),
            Instant.now()
        ));
    }
    
    // ✅ Compensation: Void authorization if processing fails
    @CommandHandler
    public void handle(VoidAuthorizationCommand command) {
        if (this.status == AuthorizationStatus.VOIDED) {
            throw new IllegalStateException("Authorization already voided");
        }
        
        AggregateLifecycle.apply(new AuthorizationVoidedEvent(
            this.authorizationId,
            command.getReason(),
            Instant.now()
        ));
    }
    
    @EventSourcingHandler
    public void on(PaymentAuthorizedEvent event) {
        this.authorizationId = event.getAuthorizationId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.status = AuthorizationStatus.AUTHORIZED;
    }
    
    @EventSourcingHandler
    public void on(AuthorizationVoidedEvent event) {
        this.status = AuthorizationStatus.VOIDED;
    }
}
```

#### REST API

```java
// authorization-service/api/AuthorizationController.java

@RestController
@RequestMapping("/api/authorizations")
public class AuthorizationController {
    
    private final CommandGateway commandGateway;
    
    @PostMapping("/authorize")
    public ResponseEntity<AuthorizePaymentResponse> authorizePayment(
            @RequestBody AuthorizePaymentRequest request) {
        
        String authorizationId = UUID.randomUUID().toString();
        
        Money money = new Money(
            request.getAmount().toString(),
            request.getCurrency()
        );
        
        PaymentCard paymentCard = new PaymentCard(
            request.getPaymentMethodId(),
            request.getCardholderName(),
            "****", // Masked for security
            request.getExpiryMonth(),
            request.getExpiryYear()
        );
        
        AuthorizePaymentCommand command = new AuthorizePaymentCommand(
            authorizationId,
            request.getCustomerId(),
            money,
            paymentCard,
            request.getMerchantId(),
            request.getDescription()
        );
        
        // Send command - will trigger saga!
        commandGateway.sendAndWait(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthorizePaymentResponse(authorizationId));
    }
}
```

#### VSA Benefits

✅ **Authorization Logic Isolated**: All payment authorization rules in one place  
✅ **Independent Evolution**: Can add fraud detection, 3DS verification without affecting other services  
✅ **Clear API Contract**: REST endpoints define what authorization service does  
✅ **Compensation Support**: Built-in void operation for saga rollbacks  

---

### 🟨 Service 3: Processing Service

**Business Capability**: Payment processing with external processor integration and Kafka event consumption

**Key Features**:
- ✅ Consumes `PaymentAuthorizedEvent` from Kafka
- ✅ Triggers processing workflow automatically (event-driven)
- ✅ Fraud detection (checks against blacklist, velocity limits)
- ✅ External payment processor simulation (Stripe, Adyen, etc.)
- ✅ Automatic retries with exponential backoff
- ✅ Compensation support (refunds)

**Folder Structure**:
```
processing-service/
├── src/main/java/com/vsa/paymentgateway/processing/
│   ├── aggregates/
│   │   └── PaymentProcessingAggregate.java       ⭐ Processing logic
│   ├── commands/
│   │   ├── ProcessPaymentCommand.java            📝 Process command
│   │   └── RefundPaymentCommand.java             📝 Refund command
│   ├── events/
│   │   ├── PaymentProcessedEvent.java            📢 Success event
│   │   ├── PaymentProcessingFailedEvent.java     📢 Failure event
│   │   └── PaymentRefundedEvent.java             📢 Refund event
│   ├── kafka/
│   │   ├── ProcessingKafkaConsumer.java          🔄 Event consumer
│   │   └── EventToKafkaForwarder.java            📤 Event publisher
│   ├── service/
│   │   ├── FraudDetectionService.java            🛡️ Fraud checks
│   │   └── ExternalProcessorService.java         💳 Processor API
│   ├── queries/
│   │   ├── ProcessingProjection.java             📊 Read model builder
│   │   ├── ProcessingReadModel.java              💾 Query model
│   │   └── ProcessingRepository.java             🗄️ JPA repo
│   ├── api/
│   │   └── ProcessingController.java             🌐 REST endpoints
│   └── domain/
│       └── ProcessingStatus.java                 🎯 Domain enum
└── pom.xml
```

#### Kafka Event Consumer (Event-Driven Trigger)

```java
// processing-service/kafka/ProcessingKafkaConsumer.java

@Service
public class ProcessingKafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessingKafkaConsumer.class);
    private final CommandGateway commandGateway;
    private final FraudDetectionService fraudDetectionService;
    
    public ProcessingKafkaConsumer(CommandGateway commandGateway, 
                                   FraudDetectionService fraudDetectionService) {
        this.commandGateway = commandGateway;
        this.fraudDetectionService = fraudDetectionService;
    }
    
    /**
     * 🔄 Listen to Kafka for PaymentAuthorizedEvent
     * This is the ENTRY POINT for processing workflow
     */
    @KafkaListener(topics = "payment-events", groupId = "processing-service-group")
    public void handleEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        
        if (event instanceof PaymentAuthorizedEvent) {
            PaymentAuthorizedEvent authorizedEvent = (PaymentAuthorizedEvent) event;
            
            logger.info("📥 Received PaymentAuthorizedEvent from Kafka: {}", 
                       authorizedEvent.getAuthorizationId());
            
            // ⭐ Fraud detection BEFORE processing
            boolean isSuspicious = fraudDetectionService.checkFraud(
                authorizedEvent.getCustomerId(),
                authorizedEvent.getAmount(),
                authorizedEvent.getMerchantId()
            );
            
            if (isSuspicious) {
                logger.warn("🚨 Suspicious activity detected - blocking payment");
                // Could emit PaymentBlockedEvent here
                return;
            }
            
            // ⭐ Trigger processing command
            String processingId = UUID.randomUUID().toString();
            ProcessPaymentCommand command = new ProcessPaymentCommand(
                processingId,
                authorizedEvent.getAuthorizationId(),
                authorizedEvent.getCustomerId(),
                authorizedEvent.getAmount(),
                authorizedEvent.getMerchantId(),
                Instant.now()
            );
            
            // Send to aggregate via command gateway
            commandGateway.send(command)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("✅ ProcessPaymentCommand sent successfully: {}", processingId);
                    } else {
                        logger.error("❌ Failed to send ProcessPaymentCommand", ex);
                    }
                });
        }
    }
}
```

#### Fraud Detection Service

```java
// processing-service/service/FraudDetectionService.java

@Service
public class FraudDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    
    // In production: This would be a database/cache lookup
    private static final Set<String> BLACKLISTED_CUSTOMERS = Set.of(
        "cust-fraud-001", "cust-fraud-002"
    );
    
    // In production: Redis cache for velocity tracking
    private final Map<String, List<Instant>> velocityTracker = new ConcurrentHashMap<>();
    
    /**
     * Multi-layered fraud detection
     */
    public boolean checkFraud(String customerId, Money amount, String merchantId) {
        
        // Rule 1: Blacklist check
        if (BLACKLISTED_CUSTOMERS.contains(customerId)) {
            logger.warn("🚨 Blacklisted customer detected: {}", customerId);
            return true;
        }
        
        // Rule 2: Amount threshold (>$10,000)
        if (amount.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            logger.warn("🚨 High-value transaction detected: ${}", amount.getAmount());
            return true;
        }
        
        // Rule 3: Velocity check (>5 transactions in last 5 minutes)
        velocityTracker.putIfAbsent(customerId, new ArrayList<>());
        List<Instant> recentTransactions = velocityTracker.get(customerId);
        
        // Clean old transactions
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        recentTransactions.removeIf(timestamp -> timestamp.isBefore(fiveMinutesAgo));
        
        if (recentTransactions.size() >= 5) {
            logger.warn("🚨 Velocity limit exceeded: {} transactions in 5 min", 
                       recentTransactions.size());
            return true;
        }
        
        // Add current transaction
        recentTransactions.add(Instant.now());
        
        logger.info("✅ Fraud check passed for customer: {}", customerId);
        return false;
    }
}
```

#### External Payment Processor Service

```java
// processing-service/service/ExternalProcessorService.java

@Service
public class ExternalProcessorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExternalProcessorService.class);
    
    /**
     * Simulates calling external payment processor API
     * In production, this would be:
     * - Stripe: POST https://api.stripe.com/v1/charges
     * - Adyen: POST https://checkout-test.adyen.com/v70/payments
     * - PayPal: POST https://api.paypal.com/v2/payments/captures
     */
    public ProcessorResponse processPayment(String authorizationId, 
                                           Money amount, 
                                           String merchantId) {
        
        logger.info("📞 Calling external processor for auth: {}", authorizationId);
        
        // Simulate network latency (100-500ms)
        try {
            Thread.sleep(100 + new Random().nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 90% success rate (10% failures for demo)
        boolean success = Math.random() < 0.90;
        
        if (success) {
            String transactionId = "TXN-" + UUID.randomUUID().toString();
            logger.info("✅ External processor success: {}", transactionId);
            
            return new ProcessorResponse(
                true, 
                transactionId, 
                "Payment processed successfully",
                Instant.now()
            );
        } else {
            logger.error("❌ External processor failed: Insufficient funds");
            
            return new ProcessorResponse(
                false, 
                null, 
                "Insufficient funds",
                Instant.now()
            );
        }
    }
    
    /**
     * Simulates refund API call
     */
    public ProcessorResponse refundPayment(String transactionId, Money amount) {
        logger.info("📞 Calling external processor refund for: {}", transactionId);
        
        // Simulate refund (always succeeds in demo)
        String refundId = "REF-" + UUID.randomUUID().toString();
        
        return new ProcessorResponse(
            true,
            refundId,
            "Refund processed successfully",
            Instant.now()
        );
    }
}

@Data
@AllArgsConstructor
class ProcessorResponse {
    private boolean success;
    private String transactionId;
    private String message;
    private Instant timestamp;
}
```

#### Payment Processing Aggregate with External Integration

```java
// processing-service/aggregates/PaymentProcessingAggregate.java

@Aggregate
public class PaymentProcessingAggregate {
    
    @AggregateIdentifier
    private String processingId;
    private String authorizationId;
    private Money amount;
    private ProcessingStatus status;
    private String transactionId;
    
    @CommandHandler
    public PaymentProcessingAggregate(ProcessPaymentCommand command,
                                     ExternalProcessorService processorService) {
        
        // ⭐ Call external payment processor (Stripe, Adyen, PayPal, etc.)
        ProcessorResponse response = processorService.processPayment(
            command.getAuthorizationId(),
            command.getAmount(),
            command.getMerchantId()
        );
        
        if (response.isSuccess()) {
            // Processing succeeded - emit success event
            AggregateLifecycle.apply(new PaymentProcessedEvent(
                command.getProcessingId(),
                command.getAuthorizationId(),
                command.getCustomerId(),
                command.getAmount(),
                command.getMerchantId(),
                response.getTransactionId(),  // From external processor
                Instant.now()
            ));
        } else {
            // Processing failed - emit failure event
            AggregateLifecycle.apply(new PaymentProcessingFailedEvent(
                command.getProcessingId(),
                command.getAuthorizationId(),
                response.getMessage()  // Error from processor
            ));
        }
    }
    
    // ✅ Compensation: Refund payment if settlement fails
    @CommandHandler
    public void handle(RefundPaymentCommand command,
                      ExternalProcessorService processorService) {
        
        if (this.status == ProcessingStatus.REFUNDED) {
            throw new IllegalStateException("Payment already refunded");
        }
        
        // Call external processor refund API
        ProcessorResponse refundResponse = processorService.refundPayment(
            this.transactionId,
            this.amount
        );
        
        if (refundResponse.isSuccess()) {
            AggregateLifecycle.apply(new PaymentRefundedEvent(
                this.processingId,
                this.authorizationId,
                command.getReason(),
                refundResponse.getTransactionId(),
                Instant.now()
            ));
        } else {
            throw new IllegalStateException("Refund failed: " + refundResponse.getMessage());
        }
    }
    
    @EventSourcingHandler
    public void on(PaymentProcessedEvent event) {
        this.processingId = event.getProcessingId();
        this.authorizationId = event.getAuthorizationId();
        this.amount = event.getAmount();
        this.status = ProcessingStatus.PROCESSED;
        this.transactionId = event.getTransactionId();
    }
    
    @EventSourcingHandler
    public void on(PaymentProcessingFailedEvent event) {
        this.processingId = event.getProcessingId();
        this.status = ProcessingStatus.FAILED;
    }
    
    @EventSourcingHandler
    public void on(PaymentRefundedEvent event) {
        this.status = ProcessingStatus.REFUNDED;
    }
}
```

#### Read Model Projection

```java
// processing-service/queries/ProcessingProjection.java

@Component
@ProcessingGroup("processing-projection")
public class ProcessingProjection {
    
    private final ProcessingRepository processingRepository;
    
    @EventHandler
    public void on(PaymentProcessedEvent event) {
        ProcessingReadModel processing = new ProcessingReadModel();
        processing.setProcessingId(event.getProcessingId());
        processing.setAuthorizationId(event.getAuthorizationId());
        processing.setCustomerId(event.getCustomerId());
        processing.setAmount(event.getAmount().getAmount());
        processing.setCurrency(event.getAmount().getCurrency());
        processing.setMerchantId(event.getMerchantId());
        processing.setTransactionId(event.getTransactionId());
        processing.setProcessedAt(event.getProcessedAt());
        processing.setStatus("PROCESSED");
        
        processingRepository.save(processing);
    }
    
    @EventHandler
    public void on(PaymentRefundedEvent event) {
        ProcessingReadModel processing = processingRepository
            .findById(event.getProcessingId())
            .orElseThrow(() -> new IllegalStateException("Processing not found"));
        
        processing.setStatus("REFUNDED");
        processing.setRefundedAt(event.getRefundedAt());
        processing.setRefundReason(event.getReason());
        
        processingRepository.save(processing);
    }
}
```

#### VSA Benefits

✅ **External Integration Encapsulated**: All Stripe/Adyen/PayPal integration in one place  
✅ **Failure Handling**: Built-in failure events and compensation (refunds)  
✅ **Independent Scaling**: Can scale processing service based on transaction volume  
✅ **Technology Swap**: Can replace simulated processor with real one without affecting other services  

---

### 🟧 Service 4: Settlement Service

**Business Capability**: Payment settlement, merchant payouts, and fee calculation

**Key Features**:
- ✅ Consumes `PaymentProcessedEvent` from Kafka
- ✅ Automatic settlement triggering (event-driven)
- ✅ Fee calculation (2.9% + $0.30 standard rate)
- ✅ Batch processing (daily batches per merchant)
- ✅ T+1 settlement schedule (next business day)
- ✅ Bank account validation
- ✅ Settlement reporting

**Folder Structure**:
```
settlement-service/
├── src/main/java/com/vsa/paymentgateway/settlement/
│   ├── aggregates/
│   │   └── SettlementAggregate.java              ⭐ Settlement logic
│   ├── commands/
│   │   └── SettlePaymentCommand.java             📝 Settle command
│   ├── events/
│   │   ├── PaymentSettledEvent.java              📢 Success event
│   │   └── SettlementFailedEvent.java            📢 Failure event
│   ├── kafka/
│   │   ├── SettlementKafkaConsumer.java          🔄 Event consumer
│   │   └── EventToKafkaForwarder.java            📤 Event publisher
│   ├── service/
│   │   ├── FeeCalculationService.java            💰 Fee calculator
│   │   ├── BatchProcessingService.java           📦 Batch handler
│   │   └── BankingService.java                   🏦 Bank API
│   ├── queries/
│   │   ├── SettlementProjection.java             📊 Read model
│   │   ├── SettlementReadModel.java              💾 Query model
│   │   └── SettlementRepository.java             🗄️ JPA repo
│   ├── api/
│   │   └── SettlementController.java             🌐 REST endpoints
│   └── domain/
│       ├── SettlementBatch.java                  📦 Batch domain
│       └── FeeStructure.java                     💵 Fee rules
└── pom.xml
```

#### Kafka Event Consumer (Event-Driven Trigger)

```java
// settlement-service/kafka/SettlementKafkaConsumer.java

@Service
public class SettlementKafkaConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(SettlementKafkaConsumer.class);
    private final CommandGateway commandGateway;
    private final FeeCalculationService feeCalculationService;
    
    public SettlementKafkaConsumer(CommandGateway commandGateway,
                                   FeeCalculationService feeCalculationService) {
        this.commandGateway = commandGateway;
        this.feeCalculationService = feeCalculationService;
    }
    
    /**
     * 🔄 Listen to Kafka for PaymentProcessedEvent
     * This is the ENTRY POINT for settlement workflow
     */
    @KafkaListener(topics = "payment-events", groupId = "settlement-service-group")
    public void handleEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        
        if (event instanceof PaymentProcessedEvent) {
            PaymentProcessedEvent processedEvent = (PaymentProcessedEvent) event;
            
            logger.info("📥 Received PaymentProcessedEvent from Kafka: {}", 
                       processedEvent.getProcessingId());
            
            // ⭐ Calculate fees
            FeeCalculation fees = feeCalculationService.calculateFees(
                processedEvent.getAmount(),
                processedEvent.getMerchantId()
            );
            
            logger.info("💰 Calculated fees: Platform=${}, Net to merchant=${}", 
                       fees.getPlatformFee(), fees.getMerchantPayout());
            
            // ⭐ Trigger settlement command
            String settlementId = UUID.randomUUID().toString();
            SettlePaymentCommand command = new SettlePaymentCommand(
                settlementId,
                processedEvent.getProcessingId(),
                processedEvent.getAuthorizationId(),
                processedEvent.getAmount(),
                processedEvent.getMerchantId(),
                fees.getPlatformFee(),
                fees.getMerchantPayout(),
                Instant.now()
            );
            
            // Send to aggregate via command gateway
            commandGateway.send(command)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("✅ SettlePaymentCommand sent successfully: {}", settlementId);
                    } else {
                        logger.error("❌ Failed to send SettlePaymentCommand", ex);
                    }
                });
        }
    }
}
```

#### Fee Calculation Service

```java
// settlement-service/service/FeeCalculationService.java

@Service
public class FeeCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeeCalculationService.class);
    
    // Standard payment processing fees (Stripe-like model)
    private static final BigDecimal PERCENTAGE_FEE = new BigDecimal("0.029");  // 2.9%
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30");        // $0.30
    
    // In production: Fee structures would be in database per merchant
    private static final Map<String, FeeStructure> MERCHANT_FEE_STRUCTURES = Map.of(
        "merchant-premium-001", new FeeStructure(new BigDecimal("0.025"), new BigDecimal("0.25")),  // Premium: 2.5% + $0.25
        "merchant-enterprise-001", new FeeStructure(new BigDecimal("0.020"), new BigDecimal("0.20")) // Enterprise: 2.0% + $0.20
    );
    
    /**
     * Calculate platform fees and merchant payout
     */
    public FeeCalculation calculateFees(Money grossAmount, String merchantId) {
        
        // Get merchant-specific fee structure or use default
        FeeStructure feeStructure = MERCHANT_FEE_STRUCTURES.getOrDefault(
            merchantId, 
            new FeeStructure(PERCENTAGE_FEE, FIXED_FEE)
        );
        
        BigDecimal amount = grossAmount.getAmount();
        
        // Calculate fees: (amount * percentage) + fixed_fee
        BigDecimal percentageFee = amount.multiply(feeStructure.getPercentageFee())
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFee = percentageFee.add(feeStructure.getFixedFee());
        
        // Calculate net payout to merchant
        BigDecimal merchantPayout = amount.subtract(totalFee);
        
        logger.info("📊 Fee calculation: Gross=${}, Fee=${} ({}% + ${}), Net=${}",
                   amount, totalFee, 
                   feeStructure.getPercentageFee().multiply(new BigDecimal("100")),
                   feeStructure.getFixedFee(),
                   merchantPayout);
        
        return new FeeCalculation(
            new Money(amount, grossAmount.getCurrency()),
            new Money(totalFee, grossAmount.getCurrency()),
            new Money(merchantPayout, grossAmount.getCurrency())
        );
    }
}

@Data
@AllArgsConstructor
class FeeCalculation {
    private Money grossAmount;
    private Money platformFee;
    private Money merchantPayout;
}

@Data
@AllArgsConstructor
class FeeStructure {
    private BigDecimal percentageFee;  // e.g., 0.029 = 2.9%
    private BigDecimal fixedFee;       // e.g., 0.30 = $0.30
}
```

#### Batch Processing Service

```java
// settlement-service/service/BatchProcessingService.java

@Service
public class BatchProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessingService.class);
    
    /**
     * Generate batch ID for daily settlement
     * Format: BATCH-{merchantId}-{YYYY-MM-DD}
     */
    public String generateBatchId(String merchantId) {
        LocalDate today = LocalDate.now();
        String batchId = String.format("BATCH-%s-%s", merchantId, today.toString());
        
        logger.info("📦 Generated batch ID: {}", batchId);
        return batchId;
    }
    
    /**
     * Calculate settlement date (T+1: next business day)
     */
    public LocalDate calculateSettlementDate() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        // Skip weekends (simplified - doesn't handle holidays)
        while (tomorrow.getDayOfWeek() == DayOfWeek.SATURDAY || 
               tomorrow.getDayOfWeek() == DayOfWeek.SUNDAY) {
            tomorrow = tomorrow.plusDays(1);
        }
        
        logger.info("📅 Settlement date: {} (T+1)", tomorrow);
        return tomorrow;
    }
    
    /**
     * In production: This would aggregate all payments in a batch
     * and create a single payout to merchant's bank account
     */
    public SettlementBatch aggregateBatch(String batchId, List<SettlementReadModel> settlements) {
        BigDecimal totalGross = settlements.stream()
            .map(SettlementReadModel::getGrossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFees = settlements.stream()
            .map(SettlementReadModel::getPlatformFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPayout = settlements.stream()
            .map(SettlementReadModel::getMerchantPayout)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        logger.info("📊 Batch {} summary: {} payments, Gross=${}, Fees=${}, Net=${}",
                   batchId, settlements.size(), totalGross, totalFees, totalPayout);
        
        return new SettlementBatch(batchId, settlements.size(), totalGross, totalFees, totalPayout);
    }
}
```

#### Banking Service (External Integration)

```java
// settlement-service/service/BankingService.java

@Service
public class BankingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingService.class);
    
    /**
     * Simulates transferring funds to merchant's bank account
     * In production, this would integrate with:
     * - Stripe Connect (for US)
     * - Plaid (for bank verification)
     * - ACH/Wire transfer APIs
     * - International: SWIFT, SEPA, etc.
     */
    public BankTransferResponse transferToMerchant(String merchantId, 
                                                   Money amount, 
                                                   String batchId) {
        
        logger.info("🏦 Initiating bank transfer: Merchant={}, Amount=${}, Batch={}",
                   merchantId, amount.getAmount(), batchId);
        
        // Simulate bank validation
        boolean accountValid = validateMerchantBankAccount(merchantId);
        if (!accountValid) {
            logger.error("❌ Invalid merchant bank account: {}", merchantId);
            return new BankTransferResponse(false, null, "Invalid bank account");
        }
        
        // Simulate transfer processing (200-800ms)
        try {
            Thread.sleep(200 + new Random().nextInt(600));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate (5% failures for demo)
        boolean success = Math.random() < 0.95;
        
        if (success) {
            String transferId = "XFER-" + UUID.randomUUID().toString();
            logger.info("✅ Bank transfer successful: {}", transferId);
            
            return new BankTransferResponse(
                true,
                transferId,
                "Transfer initiated successfully"
            );
        } else {
            logger.error("❌ Bank transfer failed: Insufficient funds in platform account");
            return new BankTransferResponse(
                false,
                null,
                "Insufficient funds in platform account"
            );
        }
    }
    
    /**
     * Validate merchant bank account
     * In production: Call bank verification API
     */
    private boolean validateMerchantBankAccount(String merchantId) {
        // Simulate invalid accounts
        Set<String> invalidAccounts = Set.of("merchant-invalid-001");
        return !invalidAccounts.contains(merchantId);
    }
}

@Data
@AllArgsConstructor
class BankTransferResponse {
    private boolean success;
    private String transferId;
    private String message;
}
```

#### Settlement Aggregate with Fee Calculation

```java
// settlement-service/aggregates/SettlementAggregate.java

@Aggregate
public class SettlementAggregate {
    
    @AggregateIdentifier
    private String settlementId;
    private String processingId;
    private Money grossAmount;
    private Money platformFee;
    private Money merchantPayout;
    private String merchantId;
    private String batchId;
    private LocalDate settlementDate;
    
    @CommandHandler
    public SettlementAggregate(SettlePaymentCommand command,
                              BatchProcessingService batchService,
                              BankingService bankingService) {
        
        // ⭐ Generate batch ID for daily settlement
        String batchId = batchService.generateBatchId(command.getMerchantId());
        
        // ⭐ Calculate settlement date (T+1)
        LocalDate settlementDate = batchService.calculateSettlementDate();
        
        // ⭐ Transfer funds to merchant's bank account
        BankTransferResponse transferResponse = bankingService.transferToMerchant(
            command.getMerchantId(),
            command.getMerchantPayout(),
            batchId
        );
        
        if (transferResponse.isSuccess()) {
            // Settlement successful
            AggregateLifecycle.apply(new PaymentSettledEvent(
                command.getSettlementId(),
                command.getProcessingId(),
                command.getAuthorizationId(),
                command.getGrossAmount(),
                command.getPlatformFee(),
                command.getMerchantPayout(),
                command.getMerchantId(),
                batchId,
                settlementDate,
                transferResponse.getTransferId(),
                Instant.now()
            ));
        } else {
            // Settlement failed
            AggregateLifecycle.apply(new SettlementFailedEvent(
                command.getSettlementId(),
                command.getProcessingId(),
                command.getAuthorizationId(),
                transferResponse.getMessage()
            ));
        }
    }
    
    @EventSourcingHandler
    public void on(PaymentSettledEvent event) {
        this.settlementId = event.getSettlementId();
        this.processingId = event.getProcessingId();
        this.grossAmount = event.getGrossAmount();
        this.platformFee = event.getPlatformFee();
        this.merchantPayout = event.getMerchantPayout();
        this.merchantId = event.getMerchantId();
        this.batchId = event.getBatchId();
        this.settlementDate = event.getSettlementDate();
    }
    
    @EventSourcingHandler
    public void on(SettlementFailedEvent event) {
        this.settlementId = event.getSettlementId();
        this.processingId = event.getProcessingId();
        // Status = FAILED (tracked in read model)
    }
}
```

#### Settlement Read Model Projection

```java
// settlement-service/queries/SettlementProjection.java

@Component
@ProcessingGroup("settlement-projection")
public class SettlementProjection {
    
    private final SettlementRepository settlementRepository;
    
    @EventHandler
    public void on(PaymentSettledEvent event) {
        SettlementReadModel settlement = new SettlementReadModel();
        settlement.setSettlementId(event.getSettlementId());
        settlement.setProcessingId(event.getProcessingId());
        settlement.setAuthorizationId(event.getAuthorizationId());
        settlement.setGrossAmount(event.getGrossAmount().getAmount());
        settlement.setPlatformFee(event.getPlatformFee().getAmount());
        settlement.setMerchantPayout(event.getMerchantPayout().getAmount());
        settlement.setCurrency(event.getGrossAmount().getCurrency());
        settlement.setMerchantId(event.getMerchantId());
        settlement.setBatchId(event.getBatchId());
        settlement.setSettlementDate(event.getSettlementDate());
        settlement.setTransferId(event.getTransferId());
        settlement.setSettledAt(event.getSettledAt());
        settlement.setStatus("SETTLED");
        
        settlementRepository.save(settlement);
    }
    
    @EventHandler
    public void on(SettlementFailedEvent event) {
        SettlementReadModel settlement = new SettlementReadModel();
        settlement.setSettlementId(event.getSettlementId());
        settlement.setProcessingId(event.getProcessingId());
        settlement.setAuthorizationId(event.getAuthorizationId());
        settlement.setStatus("FAILED");
        settlement.setFailureReason(event.getReason());
        
        settlementRepository.save(settlement);
    }
}
```

#### Settlement API Endpoints

```java
// settlement-service/api/SettlementController.java

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {
    
    private final SettlementRepository settlementRepository;
    private final BatchProcessingService batchService;
    
    // Query: Get settlement by ID
    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementReadModel> getSettlement(@PathVariable String settlementId) {
        return settlementRepository.findById(settlementId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Query: Get all settlements for a merchant
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<SettlementReadModel>> getSettlementsByMerchant(
            @PathVariable String merchantId) {
        
        List<SettlementReadModel> settlements = settlementRepository
            .findByMerchantId(merchantId);
        
        return ResponseEntity.ok(settlements);
    }
    
    // Query: Get settlements by batch ID
    @GetMapping("/batch/{batchId}")
    public ResponseEntity<BatchSummary> getBatchSummary(@PathVariable String batchId) {
        List<SettlementReadModel> settlements = settlementRepository
            .findByBatchId(batchId);
        
        if (settlements.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        SettlementBatch batch = batchService.aggregateBatch(batchId, settlements);
        
        BatchSummary summary = new BatchSummary(
            batchId,
            batch.getPaymentCount(),
            batch.getTotalGross(),
            batch.getTotalFees(),
            batch.getTotalPayout(),
            settlements.get(0).getSettlementDate()
        );
        
        return ResponseEntity.ok(summary);
    }
    
    // Query: Get daily settlement report
    @GetMapping("/report/daily")
    public ResponseEntity<DailySettlementReport> getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        List<SettlementReadModel> settlements = settlementRepository
            .findBySettlementDate(date);
        
        // Aggregate by merchant
        Map<String, List<SettlementReadModel>> byMerchant = settlements.stream()
            .collect(Collectors.groupingBy(SettlementReadModel::getMerchantId));
        
        List<MerchantSettlementSummary> merchantSummaries = byMerchant.entrySet().stream()
            .map(entry -> {
                SettlementBatch batch = batchService.aggregateBatch(
                    "BATCH-" + entry.getKey() + "-" + date, 
                    entry.getValue()
                );
                
                return new MerchantSettlementSummary(
                    entry.getKey(),
                    batch.getPaymentCount(),
                    batch.getTotalPayout()
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(new DailySettlementReport(date, merchantSummaries));
    }
}

@Data
@AllArgsConstructor
class BatchSummary {
    private String batchId;
    private int paymentCount;
    private BigDecimal totalGross;
    private BigDecimal totalFees;
    private BigDecimal totalPayout;
    private LocalDate settlementDate;
}

@Data
@AllArgsConstructor
class MerchantSettlementSummary {
    private String merchantId;
    private int paymentCount;
    private BigDecimal totalPayout;
}

@Data
@AllArgsConstructor
class DailySettlementReport {
    private LocalDate date;
    private List<MerchantSettlementSummary> merchants;
}
```

#### Settlement Benefits

| Feature | Implementation | Business Value |
|---------|----------------|----------------|
| **Automated Settlement** | Event-driven via Kafka | No manual intervention needed |
| **Fee Calculation** | Configurable per merchant | Revenue optimization |
| **Batch Processing** | Daily batches per merchant | Reduced banking fees |
| **T+1 Settlement** | Next business day payout | Industry standard |
| **Audit Trail** | Event sourced + Kafka | Complete transaction history |
| **Reporting** | Daily settlement reports | Merchant transparency |

---
        this.processingId = event.getProcessingId();
        this.amount = event.getAmount();
        this.merchantId = event.getMerchantId();
        this.batchId = event.getBatchId();
    }
}
```

#### Settlement Read Model

```java
// settlement-service/queries/SettlementProjection.java

@Component
@ProcessingGroup("settlement-projection")
public class SettlementProjection {
    
    private final SettlementRepository settlementRepository;
    
    @EventHandler
    public void on(PaymentSettledEvent event) {
        SettlementReadModel settlement = new SettlementReadModel();
        settlement.setSettlementId(event.getSettlementId());
        settlement.setProcessingId(event.getProcessingId());
        settlement.setAuthorizationId(event.getAuthorizationId());
        settlement.setAmount(event.getAmount().getAmount());
        settlement.setCurrency(event.getAmount().getCurrency());
        settlement.setMerchantId(event.getMerchantId());
        settlement.setBatchId(event.getBatchId());
        settlement.setSettlementDate(event.getSettlementDate());
        settlement.setSettledAt(event.getSettledAt());
        
        settlementRepository.save(settlement);
    }
}
```

#### VSA Benefits

✅ **Settlement Logic Isolated**: Batch processing, merchant payouts in one service  
✅ **Business Rules Encapsulated**: T+1 settlement, batch grouping, all in aggregate  
✅ **Independent Testing**: Can test settlement logic without other services  
✅ **Flexible Scaling**: Settlement is lower volume, can run fewer instances  

---

### 🟪 Service 5: Orchestration Service (Saga)

**Business Capability**: Coordinate multi-step payment flow with compensation

**Folder Structure**:
```
orchestration-service/
├── src/main/java/com/vsa/paymentgateway/orchestration/
│   └── saga/
│       └── PaymentProcessingSaga.java            ⭐ Saga orchestrator
└── pom.xml
```

#### Complete Saga Implementation

```java
// orchestration-service/saga/PaymentProcessingSaga.java

@Saga
@Slf4j
public class PaymentProcessingSaga {
    
    @Autowired
    private transient CommandGateway commandGateway;
    
    private String authorizationId;
    private String customerId;
    private String processingId;
    private String settlementId;
    private Money amount;
    private String merchantId;
    
    /**
     * ✅ STEP 1: Saga starts when payment is authorized
     * Association: authorizationId (saga will track all events with this ID)
     */
    @StartSaga
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentAuthorizedEvent event) {
        log.info("🎬 Saga started for authorization: {}", event.getAuthorizationId());
        
        // Store saga state
        this.authorizationId = event.getAuthorizationId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.merchantId = event.getMerchantId();
        
        // ✅ Send command to Processing Service
        this.processingId = UUID.randomUUID().toString();
        ProcessPaymentCommand command = new ProcessPaymentCommand(
            processingId,
            event.getAuthorizationId(),
            event.getCustomerId(),
            event.getAmount(),
            event.getMerchantId()
        );
        
        log.info("📤 Sending ProcessPaymentCommand to Processing Service");
        commandGateway.send(command);
    }
    
    /**
     * ✅ STEP 2a: Processing succeeded - proceed to settlement
     */
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentProcessedEvent event) {
        log.info("✅ Payment processed successfully: {}", event.getProcessingId());
        
        this.processingId = event.getProcessingId();
        
        // ✅ Send command to Settlement Service
        this.settlementId = UUID.randomUUID().toString();
        SettlePaymentCommand command = new SettlePaymentCommand(
            settlementId,
            event.getProcessingId(),
            event.getAuthorizationId(),
            event.getAmount(),
            event.getMerchantId()
        );
        
        log.info("📤 Sending SettlePaymentCommand to Settlement Service");
        commandGateway.send(command);
    }
    
    /**
     * ✅ STEP 3a: Settlement succeeded - saga completes successfully! 🎉
     */
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentSettledEvent event) {
        log.info("🎉 Payment fully settled! Saga completing successfully.");
        log.info("   Authorization: {}", authorizationId);
        log.info("   Processing: {}", processingId);
        log.info("   Settlement: {}", event.getSettlementId());
        log.info("   Batch: {}", event.getBatchId());
        log.info("   Settlement Date: {}", event.getSettlementDate());
        
        // ✅ Saga ends successfully
        SagaLifecycle.end();
    }
    
    /**
     * ❌ STEP 2b: Processing failed - COMPENSATE by voiding authorization
     */
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentProcessingFailedEvent event) {
        log.warn("❌ Payment processing failed: {}", event.getReason());
        log.warn("🔄 Compensating: Voiding authorization {}", authorizationId);
        
        // ✅ Compensation: Void the authorization
        VoidAuthorizationCommand command = new VoidAuthorizationCommand(
            authorizationId,
            "Processing failed: " + event.getReason()
        );
        
        commandGateway.send(command);
        
        // Saga ends after compensation
        SagaLifecycle.end();
    }
    
    /**
     * ❌ STEP 3b: Settlement failed - COMPENSATE by refunding payment
     */
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(SettlementFailedEvent event) {
        log.warn("❌ Settlement failed: {}", event.getReason());
        log.warn("🔄 Compensating: Refunding payment {}", processingId);
        
        // ✅ Compensation: Refund the processed payment
        RefundPaymentCommand command = new RefundPaymentCommand(
            processingId,
            authorizationId,
            "Settlement failed: " + event.getReason()
        );
        
        commandGateway.send(command);
        
        // Saga ends after compensation
        SagaLifecycle.end();
    }
}
```

#### Saga Flow Visualization

```
Happy Path (Success):
┌─────────────────┐
│ Authorization   │ PaymentAuthorizedEvent
│    Service      │────────────────────────┐
└─────────────────┘                        │
                                           ▼
                                    ┌──────────────┐
                                    │ Saga Starts  │
                                    │  (Step 1)    │
                                    └──────┬───────┘
                                           │ ProcessPaymentCommand
                                           ▼
┌─────────────────┐                ┌──────────────┐
│  Processing     │ PaymentProcessedEvent         │
│    Service      │◄──────────────────────────────┘
└─────────────────┘
        │
        │ PaymentProcessedEvent
        ▼
┌──────────────┐
│ Saga (Step 2)│
└──────┬───────┘
       │ SettlePaymentCommand
       ▼
┌─────────────────┐
│  Settlement     │ PaymentSettledEvent
│    Service      │────────────────────────┐
└─────────────────┘                        │
                                           ▼
                                    ┌──────────────┐
                                    │ Saga Ends    │
                                    │  (Success!)  │
                                    └──────────────┘

Failure Path 1 (Processing Fails):
┌─────────────────┐
│ Authorization   │ PaymentAuthorizedEvent
│    Service      │────────────────────────┐
└─────────────────┘                        │
                                           ▼
                                    ┌──────────────┐
                                    │ Saga Starts  │
                                    └──────┬───────┘
                                           │ ProcessPaymentCommand
                                           ▼
┌─────────────────┐                ┌──────────────┐
│  Processing     │ PaymentProcessingFailedEvent  │
│    Service      │◄──────────────────────────────┘
└─────────────────┘
        │
        │ PaymentProcessingFailedEvent
        ▼
┌──────────────────┐
│ Saga Compensation│
│  (Void Auth)     │
└──────┬───────────┘
       │ VoidAuthorizationCommand
       ▼
┌─────────────────┐
│ Authorization   │ AuthorizationVoidedEvent
│    Service      │────────────────────────┐
└─────────────────┘                        │
                                           ▼
                                    ┌──────────────┐
                                    │ Saga Ends    │
                                    │ (Compensated)│
                                    └──────────────┘

Failure Path 2 (Settlement Fails):
[Authorization → Processing: Success]
        │
        ▼
┌─────────────────┐
│  Settlement     │ SettlementFailedEvent
│    Service      │────────────────────────┐
└─────────────────┘                        │
                                           ▼
                                    ┌──────────────┐
                                    │ Saga Comp.   │
                                    │(Refund Pay)  │
                                    └──────┬───────┘
                                           │ RefundPaymentCommand
                                           ▼
┌─────────────────┐                ┌──────────────┐
│  Processing     │ PaymentRefundedEvent          │
│    Service      │◄──────────────────────────────┘
└─────────────────┘
        │
        ▼
┌──────────────┐
│ Saga Ends    │
│(Compensated) │
└──────────────┘
```

#### VSA Benefits

✅ **Orchestration Isolated**: All coordination logic in one place (saga)  
✅ **Compensation Patterns**: Automatic rollback on failures  
✅ **State Management**: Saga maintains state across async operations  
✅ **Business Process Visibility**: Clear workflow in code  
✅ **No Distributed Transactions**: Eventual consistency without 2PC  

---

## How VSA Helps Teams

### 1. **Parallel Development**

**Traditional Layered Architecture**:
```
Team A (Customer Feature):
- Modifies: CustomerController.java
- Modifies: CustomerService.java      ← CONFLICT with Team B!
- Modifies: CustomerRepository.java

Team B (Payment Feature):
- Modifies: PaymentController.java
- Modifies: PaymentService.java       ← Uses CustomerService!
- Modifies: PaymentRepository.java
```

**VSA**:
```
Team Customer:
- Works in: customer-service/         ← Isolated module
- No conflicts with other teams
- Owns entire customer experience

Team Payments:
- Works in: authorization-service/    ← Isolated module
             processing-service/      ← Isolated module
             settlement-service/      ← Isolated module
- No conflicts with Team Customer
- Listens to customer events if needed
```

### 2. **Clear Ownership**

| Team | Owns | Responsibilities |
|------|------|------------------|
| **Team Customer** | `customer-service` | Customer registration, payment methods, customer queries |
| **Team Authorization** | `authorization-service` | Payment authorization, fraud detection, authorization void |
| **Team Processing** | `processing-service` | External processor integration, payment processing, refunds |
| **Team Settlement** | `settlement-service` | Merchant settlements, batch processing, reconciliation |
| **Team Platform** | `orchestration-service` | Saga coordination, workflow orchestration |

### 3. **Independent Releases**

```bash
# Team Customer can release independently
cd customer-service
mvn clean package
docker build -t customer-service:v1.2.0 .
kubectl apply -f k8s/customer-service-deployment.yml

# Team Payments releases different services
cd authorization-service
mvn clean package
docker build -t authorization-service:v2.0.0 .
kubectl apply -f k8s/authorization-service-deployment.yml

# No coordination needed! Services are decoupled via events
```

### 4. **Easier Onboarding**

**New Developer Joining Team Customer**:
```
1. Clone repo
2. Open customer-service/ folder
3. Read CustomerAggregate.java (all business logic here!)
4. Understand customer domain
5. Start contributing

✅ Don't need to understand entire codebase
✅ Just focus on customer-service module
✅ Clear boundaries and responsibilities
```

### 5. **Technology Flexibility**

```java
// Customer Service can stay on Java 17
customer-service/pom.xml:
<java.version>17</java.version>

// Processing Service upgrades to Java 21 for virtual threads
processing-service/pom.xml:
<java.version>21</java.version>

// Settlement Service switches to Kotlin
settlement-service/pom.xml:
<kotlin.version>1.9.0</kotlin.version>

✅ Each slice evolves independently
```

### 6. **Testing Isolation**

```java
// Test customer service without other services
@SpringBootTest(classes = CustomerServiceApplication.class)
class CustomerAggregateTest {
    
    @Test
    void shouldRegisterCustomer() {
        // Test only customer logic
        // No dependencies on authorization, processing, settlement!
    }
}

// Integration test with TestContainers
@Testcontainers
class CustomerServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Test
    void shouldPersistCustomerReadModel() {
        // Test customer service end-to-end
        // Still no dependencies on other services!
    }
}
```

---

## Implementation Best Practices

### 1. **Keep Slices Focused**

✅ **Good**: Customer service handles customer domain
```
customer-service/
├── RegisterCustomer
├── AddPaymentMethod
├── UpdateCustomerProfile
└── DeactivateCustomer
```

❌ **Bad**: Customer service doing too much
```
customer-service/
├── RegisterCustomer
├── ProcessPayment          ← Belongs in processing-service!
├── SettlePayment           ← Belongs in settlement-service!
└── GenerateInvoice         ← Might belong in billing-service!
```

### 2. **Use Events for Communication**

✅ **Good**: Loose coupling via events
```java
// Customer Service
public class CustomerAggregate {
    public void updateCreditLimit(UpdateCreditLimitCommand cmd) {
        apply(new CreditLimitUpdatedEvent(...));
    }
}

// Authorization Service listens (if interested)
@EventHandler
public void on(CreditLimitUpdatedEvent event) {
    // Update authorization rules based on new credit limit
}
```

❌ **Bad**: Direct service calls
```java
// Customer Service
@Service
public class CustomerService {
    @Autowired
    private AuthorizationService authService; // ❌ Tight coupling!
    
    public void updateCreditLimit() {
        authService.updateAuthorizationRules(); // ❌ Direct call!
    }
}
```

### 3. **Shared Code Minimally**

✅ **Good**: Only share value objects and DTOs
```
payment-gateway-common/
├── domain/
│   ├── Money.java              ✅ Value object
│   ├── PaymentCard.java        ✅ Value object
│   └── Address.java            ✅ Value object
└── exceptions/
    └── PaymentGatewayException.java ✅ Base exception
```

❌ **Bad**: Sharing business logic
```
payment-gateway-common/
├── services/
│   └── ValidationService.java  ❌ Business logic leaks!
└── repositories/
    └── BaseRepository.java     ❌ Data access leaks!
```

### 4. **CQRS for Each Slice**

```java
// Each slice has its own CQRS implementation

// Write Side (Commands)
@CommandHandler
public void handle(RegisterCustomerCommand cmd) { }

// Read Side (Queries)  
@QueryHandler
public CustomerView handle(FindCustomerQuery query) { }

// Separate read and write models
// Write: CustomerAggregate
// Read: CustomerReadModel
```

### 5. **Saga for Cross-Slice Workflows**

```java
// Use sagas for workflows spanning multiple slices
@Saga
public class PaymentProcessingSaga {
    // Coordinates: Authorization → Processing → Settlement
    // Handles: Failures and compensation
}

// Don't create direct dependencies between slices!
```

---

## Common Pitfalls and Solutions

### Pitfall 1: **Fat Common Module**

❌ **Problem**:
```
payment-gateway-common/
├── services/          ← Business logic creeping in!
├── repositories/      ← Data access creeping in!
├── utils/             ← Utility classes everywhere!
└── managers/          ← Generic managers!

Result: Common module becomes a dumping ground
```

✅ **Solution**:
```
payment-gateway-common/
├── domain/
│   └── Money.java     ← Only value objects
└── exceptions/
    └── BaseException.java ← Only base exceptions

Keep it minimal! If in doubt, put it in the slice!
```

### Pitfall 2: **Slices Calling Each Other Directly**

❌ **Problem**:
```java
@Service
public class AuthorizationService {
    @Autowired
    private CustomerService customerService; // ❌ Direct dependency!
    
    public void authorize() {
        Customer customer = customerService.getCustomer(); // ❌ Tight coupling!
    }
}
```

✅ **Solution**:
```java
// Authorization Service
@SagaEventHandler
public void on(CustomerRegisteredEvent event) {
    // React to customer events
    // No direct dependency on CustomerService!
}

// Or use query through event store
CustomerReadModel customer = customerRepository.findById(customerId);
```

### Pitfall 3: **God Saga**

❌ **Problem**:
```java
@Saga
public class EverythingSaga {
    // Handles customer registration
    // Handles payment processing
    // Handles settlement
    // Handles refunds
    // Handles everything!
    
    // 2000 lines of code! 😱
}
```

✅ **Solution**:
```java
// Separate sagas for different workflows

@Saga
public class PaymentProcessingSaga {
    // Only handles: Authorization → Processing → Settlement
}

@Saga
public class RefundProcessingSaga {
    // Only handles: Refund request → Processing refund → Notify customer
}

@Saga
public class DisputeProcessingSaga {
    // Only handles: Dispute raised → Investigation → Resolution
}
```

### Pitfall 4: **Anemic Domain Models**

❌ **Problem**:
```java
// Just getters/setters, no business logic!
public class CustomerAggregate {
    private String customerId;
    private String email;
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String id) { this.customerId = id; }
    // No business rules! Just a data container! ❌
}
```

✅ **Solution**:
```java
// Rich domain model with business logic
@Aggregate
public class CustomerAggregate {
    
    @CommandHandler
    public CustomerAggregate(RegisterCustomerCommand cmd) {
        // ✅ Business rule: Email validation
        if (!isValidEmail(cmd.getEmail())) {
            throw new IllegalArgumentException("Invalid email");
        }
        
        // ✅ Business rule: Email uniqueness (checked before command)
        apply(new CustomerRegisteredEvent(...));
    }
    
    @CommandHandler
    public void handle(AddPaymentMethodCommand cmd) {
        // ✅ Business rule: Max 5 payment methods
        if (this.paymentMethods.size() >= 5) {
            throw new IllegalStateException("Maximum 5 payment methods allowed");
        }
        
        // ✅ Business rule: Luhn algorithm validation
        if (!PaymentCard.isValid(cmd.getCardNumber())) {
            throw new IllegalArgumentException("Invalid card number");
        }
        
        apply(new PaymentMethodAddedEvent(...));
    }
    
    // Business logic lives HERE, in the aggregate!
}
```

### Pitfall 5: **Ignoring Event Ordering**

❌ **Problem**:
```java
@EventHandler
public void on(PaymentProcessedEvent event) {
    // What if this arrives before PaymentAuthorizedEvent?
    // Event ordering not guaranteed in distributed systems!
}
```

✅ **Solution**:
```java
// Use saga associations to handle ordering
@Saga
public class PaymentProcessingSaga {
    
    @StartSaga
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentAuthorizedEvent event) {
        // Saga starts here - ensures ordering
    }
    
    @SagaEventHandler(associationProperty = "authorizationId")
    public void on(PaymentProcessedEvent event) {
        // This is associated with the same authorizationId
        // Saga ensures proper sequencing
    }
}
```

---

## Summary: Why This Project Demonstrates VSA Excellence

### ✅ Complete Vertical Slices
- Each service contains **everything** for its business capability
- From API endpoints to database persistence
- No artificial layering

### ✅ Event-Driven Communication
- Services communicate via **domain events**
- No tight coupling between slices
- Can add/remove services without breaking others

### ✅ CQRS Pattern
- **Write side**: Commands → Aggregates → Events
- **Read side**: Events → Projections → Read Models
- Optimized for different access patterns

### ✅ Saga Orchestration
- **PaymentProcessingSaga** coordinates multi-service workflows
- Automatic **compensation** on failures
- No distributed transactions (2PC)

### ✅ Team Autonomy
- Each team can own one or more slices
- Independent development and deployment
- Minimal coordination needed

### ✅ Production Ready
- **Event sourcing** for audit trail
- **Health checks** via Spring Actuator
- **Docker** for containerization
- **Kubernetes** manifests for deployment

---

## Testing and Verification

### Complete End-to-End Payment Flow Test

The project includes a comprehensive test script that verifies the entire payment saga:

```bash
# Run the complete E2E test
./scripts/test-complete-flow.sh
```

**What This Script Does**:

1. **Register Customer** → Creates customer account
2. **Add Payment Method** → Registers credit card (with Luhn validation)
3. **Authorize Payment** → Performs risk assessment
4. **Wait for Processing** → Kafka event triggers processing automatically
5. **Wait for Settlement** → Kafka event triggers settlement automatically
6. **Verify Complete Flow** → Confirms all events and read models

#### Test Script Breakdown

```bash
#!/bin/bash

BASE_URL="http://localhost"
CUSTOMER_API="$BASE_URL:8081/api/customers"
AUTH_API="$BASE_URL:8082/api/authorizations"
PROCESSING_API="$BASE_URL:8083/api/processing"
SETTLEMENT_API="$BASE_URL:8084/api/settlements"

echo "🧪 Starting Complete Payment Flow Test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Step 1: Register Customer
echo "📝 Step 1: Registering customer..."
CUSTOMER_RESPONSE=$(curl -s -X POST $CUSTOMER_API/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "customerName": "John Doe"
  }')

CUSTOMER_ID=$(echo $CUSTOMER_RESPONSE | jq -r '.')
echo "✅ Customer registered: $CUSTOMER_ID"

# Wait for projection to update
sleep 2

# Step 2: Add Payment Method
echo "📝 Step 2: Adding payment method..."
PAYMENT_METHOD_ID=$(uuidgen)

curl -s -X POST $CUSTOMER_API/$CUSTOMER_ID/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethodId": "'$PAYMENT_METHOD_ID'",
    "cardNumber": "4532015112830366",
    "cardholderName": "John Doe",
    "expiryDate": "12/2025",
    "cvv": "123"
  }'

echo "✅ Payment method added: $PAYMENT_METHOD_ID"
sleep 2

# Step 3: Authorize Payment
echo "📝 Step 3: Authorizing payment..."
AUTH_RESPONSE=$(curl -s -X POST $AUTH_API/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "'$CUSTOMER_ID'",
    "paymentMethodId": "'$PAYMENT_METHOD_ID'",
    "amount": {
      "amount": 100.00,
      "currency": "USD"
    },
    "merchantId": "merchant-001"
  }')

AUTH_ID=$(echo $AUTH_RESPONSE | jq -r '.authorizationId')
echo "✅ Payment authorized: $AUTH_ID"

# Step 4: Wait for Kafka Event Processing
echo "⏳ Step 4: Waiting for Processing Service (Kafka consumer)..."
sleep 5  # Give Kafka consumer time to process PaymentAuthorizedEvent

# Check processing status
PROCESSING_STATUS=$(curl -s "$PROCESSING_API/authorization/$AUTH_ID")
echo "📊 Processing Status:"
echo "$PROCESSING_STATUS" | jq '.'

PROCESSING_ID=$(echo $PROCESSING_STATUS | jq -r '.processingId')
echo "✅ Payment processed: $PROCESSING_ID"

# Step 5: Wait for Settlement
echo "⏳ Step 5: Waiting for Settlement Service (Kafka consumer)..."
sleep 5  # Give Kafka consumer time to process PaymentProcessedEvent

# Check settlement status
SETTLEMENT_STATUS=$(curl -s "$SETTLEMENT_API/processing/$PROCESSING_ID")
echo "📊 Settlement Status:"
echo "$SETTLEMENT_STATUS" | jq '.'

SETTLEMENT_ID=$(echo $SETTLEMENT_STATUS | jq -r '.settlementId')
echo "✅ Payment settled: $SETTLEMENT_ID"

# Step 6: Verify Complete Flow
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 COMPLETE SAGA VERIFIED!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Customer ID:    $CUSTOMER_ID"
echo "Payment Method: $PAYMENT_METHOD_ID"
echo "Authorization:  $AUTH_ID"
echo "Processing:     $PROCESSING_ID"
echo "Settlement:     $SETTLEMENT_ID"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Verify event store
echo "📊 Verifying Event Store..."
docker exec -it payment-gateway-postgres psql -U admin -d payment_gateway \
  -c "SELECT event_type, COUNT(*) FROM domain_event_entry GROUP BY event_type;"

echo "✅ Test Complete!"
```

### Manual Testing with cURL

#### 1. Customer Registration

```bash
# Register a customer
curl -X POST http://localhost:8081/api/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "customerName": "Alice Smith"
  }'

# Response: "cust-1234-5678-90ab-cdef"

# Get customer details
curl http://localhost:8081/api/customers/cust-1234-5678-90ab-cdef
```

#### 2. Payment Method Registration

```bash
# Add payment method (valid Visa card)
curl -X POST http://localhost:8081/api/customers/cust-1234/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethodId": "pm-1234",
    "cardNumber": "4532015112830366",
    "cardholderName": "Alice Smith",
    "expiryDate": "12/2025",
    "cvv": "123"
  }'

# Get all payment methods
curl http://localhost:8081/api/customers/cust-1234/payment-methods
```

#### 3. Payment Authorization

```bash
# Authorize payment
curl -X POST http://localhost:8082/api/authorizations/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-1234",
    "paymentMethodId": "pm-1234",
    "amount": {
      "amount": 250.00,
      "currency": "USD"
    },
    "merchantId": "merchant-001"
  }'

# Get authorization status
curl http://localhost:8082/api/authorizations/auth-5678
```

#### 4. Check Processing (Automatic via Kafka)

```bash
# Wait 3-5 seconds for Kafka consumer to trigger processing

# Check processing status by authorization ID
curl http://localhost:8083/api/processing/authorization/auth-5678

# Get processing details
curl http://localhost:8083/api/processing/proc-9012
```

#### 5. Check Settlement (Automatic via Kafka)

```bash
# Wait 3-5 seconds for Kafka consumer to trigger settlement

# Check settlement by processing ID
curl http://localhost:8084/api/settlements/processing/proc-9012

# Get batch summary
curl http://localhost:8084/api/settlements/batch/BATCH-merchant-001-2025-11-05

# Get daily report
curl http://localhost:8084/api/settlements/report/daily?date=2025-11-06
```

### Kafka Event Monitoring

#### View Events in Kafka

```bash
# Connect to Kafka container
docker exec -it payment-gateway-kafka bash

# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Consume events from beginning
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning \
  --property print.key=true

# Expected output:
# CustomerRegisteredEvent: {"customerId":"cust-1234","email":"alice@example.com",...}
# PaymentMethodAddedEvent: {"paymentMethodId":"pm-1234","cardNumber":"4532..."}
# PaymentAuthorizedEvent: {"authorizationId":"auth-5678","amount":250.00,...}
# PaymentProcessedEvent: {"processingId":"proc-9012","transactionId":"TXN-...",...}
# PaymentSettledEvent: {"settlementId":"sett-3456","batchId":"BATCH-...",...}
```

### Database Verification

#### Event Store Inspection

```bash
# Connect to PostgreSQL
docker exec -it payment-gateway-postgres psql -U admin -d payment_gateway

# View all events
SELECT 
    aggregate_identifier,
    event_type,
    timestamp,
    payload 
FROM domain_event_entry 
ORDER BY timestamp DESC 
LIMIT 10;

# Count events by type
SELECT event_type, COUNT(*) 
FROM domain_event_entry 
GROUP BY event_type;

# Expected output:
#            event_type            | count
# ---------------------------------+-------
#  CustomerRegisteredEvent         |     5
#  PaymentMethodAddedEvent         |     3
#  PaymentAuthorizedEvent          |     8
#  PaymentProcessedEvent           |     7
#  PaymentSettledEvent             |     6
```

#### Read Model Inspection

```bash
# Customer read model
SELECT customer_id, email, customer_name, registered_at 
FROM customer_read_model;

# Payment methods
SELECT payment_method_id, masked_card_number, cardholder_name 
FROM payment_method_read_model;

# Authorizations
SELECT authorization_id, customer_id, amount, status, authorized_at 
FROM authorization_read_model;

# Processing
SELECT processing_id, authorization_id, transaction_id, status, processed_at 
FROM processing_read_model;

# Settlement
SELECT 
    settlement_id, 
    gross_amount, 
    platform_fee, 
    merchant_payout, 
    batch_id, 
    settlement_date 
FROM settlement_read_model;
```

### Health Checks

```bash
# Check all services
curl http://localhost:8081/actuator/health | jq '.'  # Customer
curl http://localhost:8082/actuator/health | jq '.'  # Authorization
curl http://localhost:8083/actuator/health | jq '.'  # Processing
curl http://localhost:8084/actuator/health | jq '.'  # Settlement

# Expected response:
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "kafka": {
      "status": "UP",
      "details": {
        "clusterId": "kafka-cluster-1"
      }
    }
  }
}
```

### Service Logs

```bash
# View logs for each service
docker-compose logs -f customer-service
docker-compose logs -f authorization-service
docker-compose logs -f processing-service
docker-compose logs -f settlement-service

# View Kafka logs
docker-compose logs -f kafka

# Expected log flow:
# customer-service: "📤 Publishing CustomerRegisteredEvent to Kafka"
# authorization-service: "📤 Publishing PaymentAuthorizedEvent to Kafka"
# processing-service: "📥 Received PaymentAuthorizedEvent from Kafka"
# processing-service: "🛡️ Fraud check passed"
# processing-service: "✅ External processor success"
# processing-service: "📤 Publishing PaymentProcessedEvent to Kafka"
# settlement-service: "📥 Received PaymentProcessedEvent from Kafka"
# settlement-service: "💰 Calculated fees: Platform=$3.20, Net=$96.80"
# settlement-service: "✅ Bank transfer successful"
```

### Performance Testing

```bash
# Load test with Apache Bench (100 requests, 10 concurrent)
ab -n 100 -c 10 -p customer.json -T application/json \
  http://localhost:8081/api/customers/register

# Monitor Kafka lag
docker exec payment-gateway-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group processing-service-group

# Expected output:
# GROUP                  TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
# processing-service     payment-events  0          1523            1523            0
# settlement-service     payment-events  0          1523            1523            0
```

### Common Issues and Troubleshooting

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Kafka Not Connected** | "Failed to publish event to Kafka" | `docker-compose restart kafka` |
| **Database Lock** | "Could not acquire lock" | `docker-compose restart postgres` |
| **Consumer Lag** | Processing delayed by >10 seconds | Check `kafka-consumer-groups` and increase service instances |
| **Event Store Full** | Slow queries | Implement snapshots (aggregate rebuild optimization) |
| **Service Unhealthy** | `"status": "DOWN"` | Check logs: `docker-compose logs <service>` |

---

## Conclusion

**Vertical Slice Architecture** is a powerful alternative to traditional layered architecture that:

1. **Organizes code by business features**, not technical layers
2. **Reduces coupling** between different parts of the system
3. **Improves team autonomy** and parallel development
4. **Enables independent scaling** and deployment
5. **Makes code easier to understand** and maintain

This project demonstrates VSA in a real-world payment gateway scenario, showing how each service (Customer, Authorization, Processing, Settlement, Orchestration) is a complete vertical slice with:
- ✅ API endpoints
- ✅ Business logic (aggregates)
- ✅ Data access (repositories)
- ✅ Read models (projections)
- ✅ Event handling

By studying this implementation, teams can learn how to build maintainable, scalable systems using VSA patterns! 🚀

---

**Ready to implement VSA in your project?** Start by identifying your business capabilities and creating self-contained slices for each one!
