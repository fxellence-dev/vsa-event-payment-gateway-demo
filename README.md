# Payment Gateway Vertical Slice Architecture Demo

A complete, production-ready payment gateway implementation demonstrating **Vertical Slice Architecture (VSA)** with **CQRS**, **Event Sourcing**, **Saga Pattern**, and **Event-Driven Architecture** using Java 21, Spring Boot 3, Axon Framework, Kafka, and PostgreSQL.

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
- **Kafka Integration**: Cross-service communication
- **Saga Pattern**: Orchestrates complex business processes

## 🛠️ Technology Stack

- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.2** - Production-ready framework
- **Axon Framework 4.9** - CQRS and Event Sourcing
- **Apache Kafka** - Event streaming platform
- **PostgreSQL** - Primary database
- **Docker & Kubernetes** - Containerization and orchestration
- **Maven** - Build automation
- **JUnit 5** - Testing framework

## 📁 Project Structure

```
payment-gateway-vsa/
├── payment-gateway-common/           # Shared components
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
# 1. Register customer
CUSTOMER_ID=$(curl -s -X POST http://localhost:8080/api/customers \\
  -H "Content-Type: application/json" \\
  -d '{"customerName":"Test User","email":"test@example.com","phoneNumber":"+1234567890","address":"Test Address"}')

# 2. Add payment method
curl -X POST http://localhost:8080/api/customers/$CUSTOMER_ID/payment-methods \\
  -H "Content-Type: application/json" \\
  -d '{"cardNumber":"4111111111111111","expiryMonth":"12","expiryYear":"25","cvv":"123","cardHolderName":"Test User","isDefault":true}'

# 3. Initiate payment
PAYMENT_ID=$(curl -s -X POST http://localhost:8080/api/payments \\
  -H "Content-Type: application/json" \\
  -d "{\"customerId\":\"$CUSTOMER_ID\",\"amount\":{\"amount\":\"50.00\",\"currency\":\"USD\"},\"paymentCard\":{\"cardNumber\":\"4111111111111111\",\"expiryMonth\":\"12\",\"expiryYear\":\"25\",\"cvv\":\"123\",\"cardHolderName\":\"Test User\"},\"merchantId\":\"test-merchant\",\"description\":\"Test payment\"}")

# 4. Check payment status
curl http://localhost:8080/api/payments/$PAYMENT_ID
```

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

## 📚 Additional Resources

- [Vertical Slice Architecture](https://jimmybogard.com/vertical-slice-architecture/)
- [CQRS Journey](https://docs.microsoft.com/en-us/previous-versions/msp-n-p/jj554200(v=pandp.10))
- [Axon Framework Documentation](https://docs.axoniq.io/reference-guide/)
- [Event Sourcing Pattern](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions and support:
- Create an issue in the repository
- Review the documentation
- Check the examples in the test files

---

**Happy coding with Vertical Slice Architecture! 🎉**