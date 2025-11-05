# Payment Gateway Vertical Slice Architecture Demo

A complete, production-ready payment gateway implementation demonstrating **Vertical Slice Architecture (VSA)** with **CQRS**, **Event Sourcing**, **Saga Pattern**, and **Event-Driven Architecture** using Java 21, Spring Boot 3, Axon Framework, Kafka, and PostgreSQL.

## 🚀 Quick Start

1. **Start the system**: `./start-microservices.sh`
2. **Run tests**: `./test-microservices-enhanced.sh`
3. **Stop the system**: `./stop-microservices.sh`

For detailed instructions, see [QUICK-START.md](QUICK-START.md)

## 📂 Repository Structure

```
VSA-Demo/
├── README.md                          # This file
├── QUICK-START.md                     # Quick start guide
├── VSA-IMPLEMENTATION-GUIDE.md        # Implementation guide
├── RUNBOOK.md                         # Operations runbook
├── START-HERE.md                      # Entry point for new developers
│
├── start-microservices.sh             # Start all services
├── stop-microservices.sh              # Stop all services
├── test-microservices-enhanced.sh     # Comprehensive test suite
│
├── documentation/                     # 📚 Technical documentation
│   ├── KAFKA-EVENT-INTEGRATION.md     # Event-driven architecture guide
│   ├── TEST-SCRIPT-REVIEW.md          # Test automation documentation
│   ├── STATUS-REPORT.md               # Current implementation status
│   ├── PRODUCTION-EVOLUTION-PLAN.md   # Production roadmap
│   ├── MICROSERVICES-DEPLOYMENT.md    # Deployment guide
│   └── FAQ.md                         # Frequently asked questions
│
├── scripts/                           # 🛠️ Utility scripts
│   ├── README.md                      # Scripts documentation
│   ├── check-postgres.sh              # Database health check
│   ├── demo.sh                        # Quick demo
│   ├── logs-microservices.sh          # View service logs
│   ├── monitor-kafka-events.sh        # Monitor Kafka events
│   ├── quick-test.sh                  # Quick smoke test
│   ├── test-microservices.sh          # Basic test suite
│   └── test-payment-flow.sh           # Payment flow test
│
├── archived-docs/                     # 📦 Historical documents
│   └── PHASE-*.md, BATCH-*.md         # Phase completion docs
│
├── payment-gateway-common/            # Shared components
├── payment-gateway-customer/          # Customer service
├── payment-gateway-authorization/     # Authorization service
├── payment-gateway-processing/        # Processing service
├── payment-gateway-settlement/        # Settlement service
├── payment-gateway-monolith/          # Monolith (deprecated)
└── docker-compose.yml                 # Docker orchestration
```

## 🎯 Overview

This project showcases a real-world payment gateway scenario with the following use case flow:
1. **Customer Onboarding** - Registration and payment method setup
2. **Payment Authorization** - Card verification and risk assessment
3. **Payment Processing** - Transaction execution with fraud detection
4. **Payment Settlement** - Merchant payouts and reconciliation

## 🏗️ Architecture Principles

### Vertical Slice Architecture (VSA)
Each feature is implemented as a complete vertical slice containing:
- **Commands** (write operations)
- **Events** (domain events)
- **Aggregates** (business logic)
- **Projections** (read models)
- **Controllers** (API endpoints)
- **Repositories** (data access)

### CQRS (Command Query Responsibility Segregation)
- **Write Model**: Event-sourced aggregates handle commands
- **Read Model**: JPA projections optimized for queries
- Clear separation of command and query concerns

### Event-Driven Architecture
- **Domain Events**: Published when business events occur
- **Event Handlers**: React to events and update read models
- **Kafka Integration**: Cross-service communication via event streaming
- **Saga Pattern**: Orchestrates complex business processes

## 🛠️ Technology Stack

- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.2** - Production-ready framework
- **Axon Framework 4.8** - CQRS and Event Sourcing
- **Apache Kafka 7.5** - Event streaming platform (Confluent)
- **PostgreSQL 15** - Primary database and event store
- **Docker** - Containerization
- **Maven** - Build automation
- **JUnit 5** - Testing framework
│   ├── commands/                     # Base command classes
│   ├── events/                       # Base event classes
│   ├── valueobjects/                 # Shared value objects
│   └── infrastructure/               # Infrastructure components
├── customer-service/                 # Customer onboarding slice
│   ├── commands/                     # Customer commands
│   ├── events/                       # Customer events
│   ├── aggregate/                    # Customer aggregate
│   ├── readmodel/                    # Customer read models
│   ├── projection/                   # Event projections
│   ├── repository/                   # Data repositories
│   ├── service/                      # Query services
│   └── api/                          # REST controllers
├── authorization-service/            # Payment authorization slice
├── processing-service/               # Payment processing slice
├── settlement-service/               # Payment settlement slice
├── orchestration-service/            # Saga orchestration
└── gateway-api/                      # Main application entry point
```

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.6+

### Local Development Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd payment-gateway-vsa
```

2. **Start infrastructure services**
```bash
docker-compose up -d postgres kafka zookeeper
```

3. **Build the application**
```bash
./mvnw clean package -DskipTests
```

4. **Run the application**
```bash
./mvnw spring-boot:run -pl gateway-api
```

5. **Access the application**
- Application: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health
- Kafka UI: http://localhost:8088
- Axon Server: http://localhost:8024

### Docker Deployment

1. **Build and run everything**
```bash
docker-compose up --build
```

2. **Scale the application**
```bash
docker-compose up --scale payment-gateway=3
```

## 🎮 API Usage Examples

### Customer Onboarding

**Register a Customer**
```bash
curl -X POST http://localhost:8080/api/customers \\
  -H "Content-Type: application/json" \\
  -d '{
    "customerName": "John Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1-555-123-4567",
    "address": "123 Main St, Anytown, USA"
  }'
```

**Add Payment Method**
```bash
curl -X POST http://localhost:8080/api/customers/{customerId}/payment-methods \\
  -H "Content-Type: application/json" \\
  -d '{
    "cardNumber": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "25",
    "cvv": "123",
    "cardHolderName": "John Doe",
    "isDefault": true
  }'
```

### Payment Processing

**Initiate Payment**
```bash
curl -X POST http://localhost:8080/api/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "customerId": "{customerId}",
    "amount": {
      "amount": "100.00",
      "currency": "USD"
    },
    "paymentCard": {
      "cardNumber": "4111111111111111",
      "expiryMonth": "12",
      "expiryYear": "25",
      "cvv": "123",
      "cardHolderName": "John Doe"
    },
    "merchantId": "merchant-123",
    "description": "Online purchase"
  }'
```

**Query Payment Status**
```bash
curl http://localhost:8080/api/payments/{paymentId}
```

## 🏃‍♂️ Kubernetes Deployment

### Prerequisites
- Kubernetes cluster
- kubectl configured
- Container registry access

### Deploy to Kubernetes

1. **Build and push image**
```bash
docker build -t your-registry/payment-gateway-vsa:latest .
docker push your-registry/payment-gateway-vsa:latest
```

2. **Create namespace and deploy infrastructure**
```bash
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/kafka.yaml
```

3. **Deploy application**
```bash
kubectl apply -f k8s/payment-gateway.yaml
```

4. **Verify deployment**
```bash
kubectl get pods -n payment-gateway
kubectl get services -n payment-gateway
```

## 🧪 Testing

### Comprehensive Test Suite
```bash
# Run all tests
./test-microservices-enhanced.sh

# Quick smoke test
./scripts/quick-test.sh

# Monitor Kafka events
./scripts/monitor-kafka-events.sh
```

### Unit Tests
```bash
./mvnw test
```

### Integration Tests
```bash
./mvnw verify -P integration-tests
```

### Test a Complete Payment Flow
```bash
# See scripts/test-payment-flow.sh for complete example
```

## 📚 Documentation

### Essential Guides

- **[Quick Start Guide](QUICK-START.md)** - Get up and running in 5 minutes
- **[VSA Implementation Guide](VSA-IMPLEMENTATION-GUIDE.md)** - Deep dive into VSA patterns
- **[Operations Runbook](RUNBOOK.md)** - Production operations guide
- **[Start Here](START-HERE.md)** - Entry point for new developers

### Technical Documentation

Located in `documentation/` directory:

- **[Functional Flows](documentation/FUNCTIONAL-FLOWS.md)** - Complete business flows with sequence diagrams
  - Customer registration flow
  - Payment method registration
  - Payment authorization
  - Payment processing
  - Payment settlement
  - End-to-end payment journey
  - Error handling scenarios
  - Event propagation patterns

- **[Architecture Patterns Reference](documentation/ARCHITECTURE-PATTERNS-REFERENCE.md)** - Comprehensive architecture guide
  - Vertical Slice Architecture (VSA) explained
  - CQRS pattern implementation
  - Event Sourcing deep dive
  - Saga pattern for distributed transactions
  - Event-Driven Architecture
  - Pattern comparison matrix
  - Architecture Decision Records (ADRs)
  - Best practices and guidelines

- **[Kafka Event Integration](documentation/KAFKA-EVENT-INTEGRATION.md)** - Event-driven architecture details
- **[Test Script Review](documentation/TEST-SCRIPT-REVIEW.md)** - Test automation documentation
- **[Status Report](documentation/STATUS-REPORT.md)** - Current implementation status
- **[Production Evolution Plan](documentation/PRODUCTION-EVOLUTION-PLAN.md)** - Roadmap to production
- **[Microservices Deployment](documentation/MICROSERVICES-DEPLOYMENT.md)** - Deployment guide
- **[FAQ](documentation/FAQ.md)** - Frequently asked questions

### Utility Scripts

Located in `scripts/` directory - see [scripts/README.md](scripts/README.md) for details.

## 🧪 Testing (Continued)

## 📊 Monitoring and Observability

### Health Checks
- Application: `/actuator/health`
- Database: `/actuator/health/db`
- Kafka: `/actuator/health/kafka`

### Metrics
- Prometheus metrics: `/actuator/prometheus`
- JVM metrics: `/actuator/metrics`
- Custom business metrics: Payment success rates, processing times

### Logging
- Structured JSON logging
- Correlation IDs for tracing
- Business event logging

### Dashboards
- Grafana dashboards for system metrics
- Kafka UI for message monitoring
- Axon Server dashboard for event streams

## 🎨 VSA Best Practices Demonstrated

### 1. Feature-Focused Organization
Each vertical slice contains all layers needed for a specific business capability:
```
customer-service/
├── commands/           # What can be done
├── events/            # What happened
├── aggregate/         # Business rules
├── readmodel/         # Query optimization
├── projection/        # Event handling
├── repository/        # Data access
├── service/           # Query logic
└── api/              # External interface
```

### 2. Explicit Business Language
```java
// Commands express business intent
public class RegisterCustomerCommand extends PaymentCommand
public class AuthorizePaymentCommand extends PaymentCommand
public class SettlePaymentCommand extends PaymentCommand

// Events capture business facts
public class CustomerRegisteredEvent extends PaymentDomainEvent
public class PaymentAuthorizedEvent extends PaymentDomainEvent
public class PaymentSettledEvent extends PaymentDomainEvent
```

### 3. Aggregate-Driven Design
```java
@Aggregate
public class CustomerAggregate {
    @CommandHandler
    public CustomerAggregate(RegisterCustomerCommand command) {
        // Business validation
        validateCustomerData(command);
        
        // Apply domain event
        AggregateLifecycle.apply(new CustomerRegisteredEvent(...));
    }
}
```

### 4. CQRS Read/Write Separation
```java
// Write side: Commands handled by aggregates
@CommandHandler
public void handle(AddPaymentMethodCommand command) { ... }

// Read side: Queries handled by projections
@EventHandler
public void on(PaymentMethodAddedEvent event) { 
    // Update read model
}
```

### 5. Saga Orchestration
```java
@Saga
public class PaymentProcessingSaga {
    @StartSaga
    @SagaEventHandler
    public void handle(PaymentInitiatedEvent event) {
        // Start authorization
        commandGateway.send(new AuthorizePaymentCommand(...));
    }
    
    @SagaEventHandler
    public void handle(PaymentAuthorizedEvent event) {
        // Start processing
        commandGateway.send(new ProcessPaymentCommand(...));
    }
    
    // Handle compensating actions
    @SagaEventHandler
    public void handle(PaymentProcessingFailedEvent event) {
        // Compensate authorization
        commandGateway.send(new VoidAuthorizationCommand(...));
    }
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following VSA principles
4. Add tests for new functionality
5. Submit a pull request

## 🎓 Learning Resources

### Internal Documentation
- **[Functional Flows](documentation/FUNCTIONAL-FLOWS.md)** - Learn how the system works through detailed sequence diagrams
- **[Architecture Patterns](documentation/ARCHITECTURE-PATTERNS-REFERENCE.md)** - Understand the patterns and why we chose them

### External Resources
- [Vertical Slice Architecture](https://jimmybogard.com/vertical-slice-architecture/) - Jimmy Bogard's original concept
- [CQRS Journey](https://docs.microsoft.com/en-us/previous-versions/msp-n-p/jj554200(v=pandp.10)) - Microsoft's CQRS guide
- [Axon Framework Documentation](https://docs.axoniq.io/reference-guide/) - Official Axon docs
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html) - Martin Fowler's explanation
- [Saga Pattern](https://microservices.io/patterns/data/saga.html) - Chris Richardson's microservices patterns

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions and support:
- Create an issue in the repository
- Review the documentation
- Check the examples in the test files

---

**Happy coding with Vertical Slice Architecture! 🎉**