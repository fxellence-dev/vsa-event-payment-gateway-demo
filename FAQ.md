# VSA Payment Gateway - Frequently Asked Questions

## Architecture Questions

### Q1: Why don't I see any Java code in processing-service and settlement-service?

**A**: These are **intentionally placeholder modules** for this demo project. Here's why:

**What They Contain**:
- Only `pom.xml` files (Maven module definitions)
- No `src/` directories
- No Java code

**Why They're Placeholders**:

1. **Demo Focus**: This demo focuses on demonstrating:
   - ✅ Vertical Slice Architecture (VSA) organization
   - ✅ CQRS pattern (Command/Query separation)
   - ✅ Event Sourcing with Axon Framework
   - ✅ Saga orchestration patterns
   - ✅ JPA-based event store (without AxonServer)

2. **Complexity Avoidance**: Real implementations would require:
   - **Processing Service**:
     - Integration with payment processors (Stripe, Adyen, PayPal APIs)
     - PCI compliance for handling card data
     - 3D Secure authentication
     - Fraud detection services
     - Currency conversion
     - Retry logic and circuit breakers
   
   - **Settlement Service**:
     - Banking/ACH integration for merchant payouts
     - Batch processing for settlements
     - Reconciliation with processor reports
     - Multi-currency settlement
     - Fee calculations
     - Settlement schedules (daily, weekly, monthly)

3. **Simulated in Saga**: The `PaymentProcessingSaga` in the `orchestration-service` **simulates** what these services would do:
   ```java
   // Current Demo Behavior
   @StartSaga
   @EventHandler
   public void handle(PaymentInitiatedEvent event) {
       this.status = PaymentSagaStatus.STARTED;
       // Simulate authorization
       this.authorizationId = UUID.randomUUID().toString();
       this.status = PaymentSagaStatus.AUTHORIZED;
       // Auto-complete for demo
       this.status = PaymentSagaStatus.COMPLETED;
       SagaLifecycle.end();
   }
   ```

4. **Ready for Implementation**: The structure is there! To implement them:
   ```
   processing-service/
   └─ src/main/java/
      └─ com/vsa/paymentgateway/processing/
         ├─ aggregates/PaymentProcessingAggregate.java
         ├─ commands/ProcessPaymentCommand.java
         ├─ events/PaymentProcessedEvent.java
         ├─ events/PaymentProcessingFailedEvent.java
         └─ services/StripePaymentProcessor.java
   
   settlement-service/
   └─ src/main/java/
      └─ com/vsa/paymentgateway/settlement/
         ├─ aggregates/SettlementAggregate.java
         ├─ commands/SettlePaymentCommand.java
         ├─ events/PaymentSettledEvent.java
         └─ services/SettlementBatchProcessor.java
   ```

**What You CAN See**:
- ✅ Fully implemented: `customer-service` (Customer & Payment Methods)
- ✅ Fully implemented: `authorization-service` (Payment Authorization)
- ✅ Partially implemented: `orchestration-service` (Payment Processing Saga)

---

### Q2: What is the purpose of the orchestration service?

**A**: The orchestration service contains the **PaymentProcessingSaga** - the "conductor" or "brain" of the payment flow.

#### Core Purpose: Saga Orchestration

A **Saga** is a pattern for managing long-running business processes that span multiple services/aggregates. Instead of a single transaction, it coordinates a sequence of local transactions with compensation logic.

#### What the PaymentProcessingSaga Does

**1. Coordinates Multi-Step Payment Flow**:
```
Authorization → Processing → Settlement
```

Each step might be handled by a different aggregate/service, and the saga ensures they all complete in order.

**2. Maintains State Across Async Operations**:
```java
@Saga
public class PaymentProcessingSaga {
    // Saga state persisted in saga_entry table
    private String paymentId;
    private String authorizationId;
    private String processingId;
    private String settlementId;
    private PaymentSagaStatus status;
    
    // State survives restarts!
}
```

**3. Handles Failures with Compensation**:
```
Happy Path:
  Authorize ✓ → Process ✓ → Settle ✓ → DONE

Failure Scenarios:
  Authorize ✗ → END (nothing to compensate)
  Authorize ✓ → Process ✗ → Void Authorization → END
  Authorize ✓ → Process ✓ → Settle ✗ → Refund Payment → END
```

**4. Ensures Eventual Consistency**:
- No distributed transactions (no 2PC - Two-Phase Commit)
- Each step is a local transaction
- Saga ensures all steps complete OR all are compensated
- System is always in a consistent state (eventually)

#### Real-World Example

**Booking a Flight + Hotel + Car**:

Without Saga:
```
Book Flight → SUCCESS
Book Hotel → SUCCESS  
Book Car → FAILURE
Now what? You're stuck with flight + hotel but no car!
Manual rollback needed 😞
```

With Saga:
```
1. Book Flight → FlightBookedEvent
2. Saga: Send BookHotelCommand
3. Book Hotel → HotelBookedEvent
4. Saga: Send BookCarCommand
5. Book Car → CarBookingFailedEvent
6. Saga Compensation:
   - Send CancelHotelCommand
   - Send CancelFlightCommand
7. All bookings cancelled automatically ✅
```

#### Payment Processing Saga Flow (When Fully Implemented)

```
1. PaymentInitiatedEvent received
   ↓
2. Saga: Send AuthorizePaymentCommand
   ↓
3. PaymentAuthorizedEvent received
   ├─ Store authorizationId
   └─ Saga: Send ProcessPaymentCommand
   ↓
4a. PaymentProcessedEvent received
   ├─ Store processingId
   └─ Saga: Send SettlePaymentCommand
   ↓
5a. PaymentSettledEvent received
   ├─ Store settlementId
   └─ END SAGA (success)

4b. PaymentProcessingFailedEvent received
   ├─ Compensation needed!
   ├─ Saga: Send VoidAuthorizationCommand
   └─ END SAGA (compensated)

5b. PaymentSettlementFailedEvent received
   ├─ Compensation needed!
   ├─ Saga: Send RefundPaymentCommand
   └─ END SAGA (compensated)
```

#### Why Not Just Use a Transaction?

**Database Transactions Don't Work** for:
- ❌ Operations that take minutes/hours (settlement batch processing)
- ❌ Operations across different databases (microservices)
- ❌ Operations with external APIs (Stripe, Adyen)
- ❌ Operations that might timeout

**Sagas Work Because**:
- ✅ Each step is a separate transaction
- ✅ State is persisted between steps (in `saga_entry` table)
- ✅ Can handle timeouts (with deadline handlers)
- ✅ Can handle retries
- ✅ Can survive application restarts

#### Saga Storage in PostgreSQL

The saga state is stored in the `saga_entry` table:

```sql
SELECT * FROM saga_entry;
```

Result:
```
saga_id                              | saga_type                    | serialized_saga | ...
-------------------------------------|------------------------------|-----------------|----
a1b2c3d4-e5f6-7890-abcd-ef1234567890 | PaymentProcessingSaga        | <binary data>   | ...
```

The serialized_saga contains:
- paymentId
- customerId
- authorizationId
- status (STARTED, AUTHORIZED, PROCESSING_PENDING, etc.)
- All saga instance variables

#### Code Location

**File**: `orchestration-service/src/main/java/com/vsa/paymentgateway/orchestration/saga/PaymentProcessingSaga.java`

**Key Annotations**:
```java
@Saga  // Marks this as a saga
public class PaymentProcessingSaga {
    
    @Autowired
    private transient CommandGateway commandGateway;  // Send commands
    
    @StartSaga  // Saga starts when this event arrives
    @EventHandler
    public void handle(PaymentInitiatedEvent event) {
        // Initialize saga state
    }
    
    @EventHandler
    public void on(PaymentProcessedEvent event) {
        // Continue saga, send next command
    }
    
    @EndSaga  // Saga ends when this is called
    @EventHandler
    public void on(PaymentSettledEvent event) {
        // Saga completed successfully
        SagaLifecycle.end();
    }
}
```

---

## Architectural Comparison

### Monolith vs Microservices

**Current Architecture**: **Modular Monolith with VSA**

```
Single Deployment (gateway-api.jar)
├─ customer-service module
├─ authorization-service module
├─ orchestration-service module
├─ processing-service module (placeholder)
└─ settlement-service module (placeholder)

Single Database (PostgreSQL)
├─ Read Models (customer_read_model, authorization_read_model, ...)
├─ Event Store (domain_event_entry)
└─ Saga State (saga_entry)

Single JVM
├─ In-memory Command Bus
├─ In-memory Event Bus
└─ In-memory Query Bus
```

**Benefits**:
- ✅ Simple deployment (one JAR, one database)
- ✅ No network latency between modules
- ✅ Strong consistency within modules
- ✅ Easy to test end-to-end
- ✅ Lower operational overhead
- ✅ Easier to debug (single log, single process)

**When to Extract to Microservices**:
- ⚠️ Different scaling needs (e.g., authorization needs 10x more capacity)
- ⚠️ Different teams own different modules
- ⚠️ Different technology stacks needed
- ⚠️ Deployment independence required
- ⚠️ Regulatory/compliance isolation needed

### VSA vs Layered Architecture

**Traditional Layered** (what most people do):
```
presentation/
  └─ All controllers together
business/
  └─ All services together
data/
  └─ All repositories together
```

**Problem**: To add a feature, you touch files in 3+ different folders. Hard to see the complete picture of a feature.

**VSA** (this project):
```
customer-service/
  ├─ api/CustomerController
  ├─ aggregates/CustomerAggregate
  ├─ commands/
  ├─ events/
  ├─ queries/CustomerProjection
  └─ domain/
```

**Benefit**: Everything for "customer management" is in one place. Easy to understand, change, and test.

---

## Implementation Status

### What Works Right Now

✅ **Customer Management**:
- Register customer
- Add payment methods
- Query customers by ID or email
- Luhn validation on card numbers
- Card type detection (VISA, Mastercard, Amex)

✅ **Authorization**:
- Authorize payments
- Query authorizations
- Risk validation

✅ **Event Sourcing**:
- All domain events stored in PostgreSQL (`domain_event_entry`)
- Event replay capability
- Projections update from events

✅ **CQRS**:
- Commands change state (via aggregates)
- Queries read from read models (via repositories)
- Separate paths for writes and reads

✅ **Saga Orchestration**:
- PaymentProcessingSaga demonstrates orchestration pattern
- Saga state persisted in `saga_entry`
- Simplified flow for demo purposes

### What's Simulated/Placeholder

⚠️ **Payment Processing**:
- No real Stripe/Adyen integration
- Simulated in saga

⚠️ **Settlement**:
- No real banking integration
- Simulated in saga

⚠️ **Full Saga Flow**:
- Saga auto-completes for demo
- Production would have multiple event handlers and compensation logic

### How to Extend to Production

**1. Implement Processing Service**:
```java
@Aggregate
public class PaymentProcessingAggregate {
    
    @CommandHandler
    public PaymentProcessingAggregate(ProcessPaymentCommand cmd) {
        // Call Stripe API
        Charge charge = stripeClient.charges.create(params);
        
        if (charge.getStatus().equals("succeeded")) {
            apply(new PaymentProcessedEvent(cmd.getPaymentId(), charge.getId()));
        } else {
            apply(new PaymentProcessingFailedEvent(cmd.getPaymentId(), charge.getFailureMessage()));
        }
    }
}
```

**2. Implement Settlement Service**:
```java
@Aggregate
public class SettlementAggregate {
    
    @CommandHandler
    public void handle(SettlePaymentCommand cmd) {
        // Add to settlement batch
        settlementBatch.addPayment(cmd.getPaymentId(), cmd.getAmount());
        
        apply(new PaymentSettledEvent(cmd.getPaymentId(), batchId));
    }
}
```

**3. Update Saga**:
```java
@EventHandler
public void on(PaymentProcessedEvent event) {
    // Send next command
    commandGateway.send(new SettlePaymentCommand(paymentId, amount));
}

@EventHandler
public void on(PaymentProcessingFailedEvent event) {
    // Compensate
    commandGateway.send(new VoidAuthorizationCommand(authorizationId));
    SagaLifecycle.end();
}
```

**4. Add Monitoring**:
- Prometheus metrics
- Distributed tracing (Zipkin/Jaeger)
- Log aggregation (ELK stack)
- Alert on saga timeouts

---

## Quick Reference

### Module Structure Summary

| Module | Lines of Code | Purpose | Status |
|--------|--------------|---------|--------|
| customer-service | ~1,500 | Customer & payment methods | ✅ Production Ready |
| authorization-service | ~800 | Payment authorization | ✅ Production Ready |
| orchestration-service | ~200 | Saga orchestration | ⚠️ Demo/Simplified |
| processing-service | 0 | Payment processing | ⚠️ Placeholder |
| settlement-service | 0 | Settlement processing | ⚠️ Placeholder |
| payment-gateway-common | ~300 | Shared domain | ✅ Production Ready |
| gateway-api | ~400 | Application assembly | ✅ Production Ready |

### Key Files to Understand the Project

1. **VSA Structure**: 
   - `customer-service/src/main/java/com/vsa/paymentgateway/customer/`

2. **Event Sourcing**:
   - `gateway-api/src/main/java/com/vsa/paymentgateway/gateway/config/AxonConfig.java`

3. **Saga Pattern**:
   - `orchestration-service/src/main/java/com/vsa/paymentgateway/orchestration/saga/PaymentProcessingSaga.java`

4. **CQRS Separation**:
   - Commands: `customer-service/.../commands/`
   - Queries: `customer-service/.../queries/`

5. **Aggregates (Domain Logic)**:
   - `customer-service/.../aggregates/CustomerAggregate.java`
   - `authorization-service/.../aggregates/PaymentAuthorizationAggregate.java`

---

**Last Updated**: 2 November 2025
