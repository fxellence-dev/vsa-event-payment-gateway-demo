# Event-Driven Architecture - Kafka Integration

## Overview
This document describes the implementation of complete event-driven architecture where all domain events are published to Apache Kafka for cross-service communication.

## Implementation Status

### ✅ Completed
1. **EventToKafkaForwarder** - Simple, elegant solution for publishing events
   - Customer Service ✓
   - Authorization Service ✓
   - Processing Service ✓  
   - Settlement Service ✓

2. **Kafka Infrastructure**
   - Topic: `payment-events`
   - Bootstrap Server: `kafka:29092` (internal), `localhost:9092` (external)
   - Replication Factor: 1 (development)

3. **Event Publishing**
   - All domain events automatically forwarded to Kafka
   - JSON serialization with Jackson
   - Java 8 date/time support (JSR310 module)
   - Event type as message key
   - JSON payload as message value

4. **Test Verification**
   - Enhanced test script with Kafka event verification
   - Retry logic (3 attempts with 3-second delays)
   - Unique consumer groups to avoid conflicts
   - Support for both XML and JSON event formats

### Event Flow Architecture

```
Command → Aggregate → Event Store (PostgreSQL)
                     ↓
                  @EventHandler (EventToKafkaForwarder)
                     ↓
                  KafkaTemplate.send()
                     ↓
                  Kafka Topic (payment-events)
                     ↓
                  Other Services (consumers)
```

## EventToKafkaForwarder Pattern

### Implementation
```java
@Component
public class EventToKafkaForwarder {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    
    public EventToKafkaForwarder(KafkaTemplate<String, String> kafkaTemplate,
                                  @Value("${axon.kafka.default-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @EventHandler
    public void on(Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            System.out.println("Publishing event to Kafka: " + event.getClass().getSimpleName());
            kafkaTemplate.send(topic, event.getClass().getSimpleName(), json);
        } catch (Exception e) {
            System.err.println("Failed to forward event to Kafka: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Key Features
1. **Catch-All Handler**: `@EventHandler` with `Object` parameter catches ALL events
2. **JSON Serialization**: Jackson ObjectMapper with JSR310 module
3. **Message Structure**:
   - Key: Event class simple name (e.g., "CustomerRegisteredEvent")
   - Value: Full JSON representation of event
4. **Error Handling**: Try-catch with logging, doesn't break event flow
5. **Logging**: Console output for debugging

## Verified Events

### ✅ Working Events
- **CustomerRegisteredEvent** - Published to Kafka successfully
- **PaymentMethodAddedEvent** - Published to Kafka successfully  
- **PaymentAuthorizedEvent** - Published to Kafka successfully

### ⏸️ Pending Verification (after rebuild)
- **PaymentProcessedEvent** - Processing service rebuild in progress
- **PaymentSettledEvent** - Settlement service rebuild in progress
- **PaymentAuthorizationDeclinedEvent** - Authorization service rebuild in progress

## Event Message Format

### JSON Format (Current)
```json
{
  "eventId": "3eca3c0e-4d69-4552-9051-34d5fa2dfc75",
  "timestamp": 1762342538.364638666,
  "aggregateId": "cust-17623425383N-EE34F9EC",
  "version": 0,
  "customerName": "Test User 1762342538",
  "email": "test.user-17623425383N@test.example.com",
  "phoneNumber": "+1-555-0123",
  "address": "123 Test Street, Test City, TC 12345",
  "customerId": "cust-17623425383N-EE34F9EC"
}
```

### XML Format (Legacy - from earlier attempts)
```xml
<com.vsa.paymentgateway.customer.events.CustomerRegisteredEvent>
  <eventId>09785b51-475e-4fe9-9087-bd5367618290</eventId>
  <timestamp>2025-11-04T14:17:07.083568710Z</timestamp>
  <aggregateId>cust-17622658263N-2BDD3314</aggregateId>
  <version>0</version>
  <customerName>John Doe Test 1762265826</customerName>
  ...
</com.vsa.paymentgateway.customer.events.CustomerRegisteredEvent>
```

## Test Results

### Latest Test Run (2025-11-05)
- **Tests Run**: 5
- **Tests Passed**: 4  
- **Pass Rate**: 80%

### Kafka Event Verification
✅ CustomerRegisteredEvent for cust-17623438523N-7E66ADA8  
✅ PaymentMethodAddedEvent for cust-17623438523N-7E66ADA8  
✅ PaymentAuthorizedEvent for auth-17623438523N-A6296CBD  
⏸️ PaymentProcessedEvent (pending rebuild)  
⏸️ PaymentSettledEvent (pending rebuild)  
⏸️ PaymentAuthorizationDeclinedEvent (pending rebuild)

## Troubleshooting

### Issue 1: Events not found in Kafka
**Symptom**: Test script reports "not found" but logs show "Publishing event to Kafka"

**Root Cause**: 
- Multiple grep commands not working with single-line XML
- Insufficient wait time for async event publishing
- Consumer group conflicts

**Solution**:
1. Updated grep pattern to search for ID first, then verify event type
2. Increased initial wait time from 2s to 5s
3. Added retry logic (3 attempts with 3s delays)
4. Use unique consumer groups: `kafka-test-$(date +%s%N)-$$`

### Issue 2: Jackson serialization errors
**Symptom**: `InvalidDefinitionException: Java 8 date/time type 'java.time.Instant' not supported`

**Solution**:
```java
objectMapper.registerModule(new JavaTimeModule());
```

### Issue 3: Mixed event formats (XML and JSON)
**Symptom**: Some events in XML, some in JSON

**Root Cause**: Different serialization configurations across services

**Solution**: Standardize on JSON format with Jackson across all services

## Manual Verification Commands

### Check total messages in topic
```bash
docker exec vsa-kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic payment-events \
  --time -1
```

### Read latest N messages
```bash
docker exec vsa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --partition 0 \
  --offset <offset> \
  --max-messages <N>
```

### Search for specific event
```bash
docker exec vsa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning \
  --max-messages 200 \
  --timeout-ms 5000 \
  --group test-manual-$$-$(date +%s%N) \
  2>&1 | grep -v "Processed a total" | grep "<search-term>"
```

### Check service logs for event publishing
```bash
docker logs vsa-customer-service 2>&1 | grep -i "Publishing event"
docker logs vsa-authorization-service 2>&1 | grep -i "Publishing event"
docker logs vsa-processing-service 2>&1 | grep -i "Publishing event"
docker logs vsa-settlement-service 2>&1 | grep -i "Publishing event"
```

## Next Steps

1. ✅ Deploy EventToKafkaForwarder to all services
2. ⏸️ Rebuild and restart all services (IN PROGRESS)
3. 🔲 Run comprehensive test suite
4. 🔲 Verify all events published successfully
5. 🔲 Implement event consumers in other services
6. 🔲 Complete saga orchestration with Kafka events
7. 🔲 Add dead letter queue for failed events
8. 🔲 Implement event replay capability
9. 🔲 Add monitoring and metrics

## Architecture Benefits

1. **Loose Coupling**: Services don't need direct dependencies
2. **Scalability**: Kafka handles high throughput
3. **Resilience**: Events persisted, can replay if needed
4. **Auditability**: Complete event log for compliance
5. **Flexibility**: New services can subscribe to existing events
6. **Debugging**: Easy to inspect event flow in Kafka

## Configuration

### application.yml
```yaml
axon:
  kafka:
    default-topic: payment-events
    bootstrap-servers: kafka:29092

spring:
  kafka:
    bootstrap-servers: kafka:29092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
```

## Performance Considerations

1. **Batch Size**: Currently default (16384 bytes)
2. **Compression**: None (can enable for production)
3. **Acknowledgments**: `acks=all` for reliability
4. **Retries**: 3 attempts for transient failures
5. **Timeout**: No explicit timeout (relies on Kafka defaults)

## Security (Future Enhancement)

1. Add SASL/SSL authentication
2. Encrypt messages at rest
3. Implement authorization for topics
4. Add PII data masking/encryption
5. Audit log access to events

---

**Last Updated**: 2025-11-05  
**Status**: In Progress - Services Rebuilding  
**Next Review**: After rebuild completes and tests run
