# Quick Reference Guide - Phase 7 Testing

## 🚀 Quick Start

### Start Services
```bash
cd /Users/amitmahajan/Documents/Projects/VSA-Demo
docker-compose -f docker-compose-microservices.yml up -d
sleep 30  # Wait for services to initialize
```

### Run Tests
```bash
./test-microservices.sh
```

### View Results
The test script provides real-time color-coded output:
- 🟢 **Green (✓)** = Test passed
- 🔴 **Red (✗)** = Test failed
- 🟡 **Yellow (⚠)** = Warning
- 🔵 **Blue (ℹ)** = Info

## 📊 Check Service Health

### All Services at Once
```bash
for port in 8081 8082 8083 8084 8085; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq -r '.status')"
done
```

Expected output:
```
Port 8081: UP  (customer-service)
Port 8082: UP  (authorization-service)
Port 8083: UP  (processing-service)
Port 8084: UP  (settlement-service)
Port 8085: UP  (orchestration-service)
```

### Individual Service
```bash
curl -s http://localhost:8081/actuator/health | jq .
```

## 🔍 Monitor Services

### View Logs - All Services
```bash
docker-compose -f docker-compose-microservices.yml logs -f
```

### View Logs - Specific Service
```bash
# Authorization service
docker logs vsa-authorization-service -f

# Customer service
docker logs vsa-customer-service -f

# Processing service
docker logs vsa-processing-service -f

# Settlement service
docker logs vsa-settlement-service -f

# Orchestration service
docker logs vsa-orchestration-service -f
```

### View Last 50 Lines
```bash
docker logs vsa-authorization-service --tail=50
```

### Search Logs for Errors
```bash
docker logs vsa-authorization-service 2>&1 | grep -i error
docker logs vsa-authorization-service 2>&1 | grep -i exception
```

## 📨 Monitor Kafka Events

### View All Events
```bash
docker exec -it vsa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning
```

### View Recent Events (Last 10)
```bash
docker exec -it vsa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning \
  --max-messages 10 \
  --timeout-ms 5000 2>/dev/null
```

### View Only Authorization Events
```bash
docker exec -it vsa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-events \
  --from-beginning \
  --timeout-ms 5000 2>/dev/null | grep "PaymentAuthorizedEvent"
```

### List All Topics
```bash
docker exec -it vsa-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

## 🧪 Manual API Testing

### 1. Register Customer
```bash
curl -X POST http://localhost:8081/api/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-manual-test-001",
    "name": "John Doe",
    "email": "john.manual@test.com",
    "phone": "+1-555-0100",
    "address": "123 Test St"
  }' | jq .
```

Expected response:
```json
{
  "customerId": "cust-manual-test-001",
  "message": "Customer registered successfully"
}
```

### 2. Add Payment Method
```bash
curl -X POST http://localhost:8081/api/customers/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-manual-test-001",
    "paymentCard": {
      "cardNumber": "4111111111111111",
      "expiryMonth": "12",
      "expiryYear": "2025",
      "cvv": "123",
      "cardHolderName": "John Doe"
    },
    "isDefault": true
  }' | jq .
```

Expected response:
```json
{
  "message": "Payment method added successfully"
}
```

### 3. Authorize Payment
```bash
curl -X POST http://localhost:8082/api/authorizations/authorize \
  -H "Content-Type: application/json" \
  -d '{
    "authorizationId": "auth-manual-test-001",
    "paymentId": "pay-manual-test-001",
    "customerId": "cust-manual-test-001",
    "merchantId": "merchant-test",
    "amount": 99.99,
    "currency": "USD",
    "paymentMethodId": "4111111111111111"
  }' | jq .
```

Expected response:
```json
{
  "authorizationId": "auth-manual-test-001",
  "status": "AUTHORIZED",
  "message": "Payment authorization initiated successfully"
}
```

### 4. Query Customer (NOT YET IMPLEMENTED)
```bash
curl -X GET http://localhost:8081/api/customers/cust-manual-test-001 | jq .
```

Expected response: HTTP 404 (read model not implemented)

## 🗄️ Database Access

### Connect to PostgreSQL
```bash
docker exec -it vsa-postgres psql -U payment_user -d payment_gateway_db
```

### Check Customer Data
```sql
-- Show all schemas
\dn

-- Switch to customer schema
SET search_path TO customer_schema;

-- List all tables
\dt

-- View customers (if table exists)
SELECT * FROM customer_aggregate;
```

### Check Authorization Data
```sql
SET search_path TO authorization_schema;
\dt
SELECT * FROM payment_authorization_aggregate;
```

### Exit PostgreSQL
```sql
\q
```

## 🔄 Restart Services

### Restart All
```bash
docker-compose -f docker-compose-microservices.yml restart
```

### Restart Specific Service
```bash
docker-compose -f docker-compose-microservices.yml restart authorization-service
```

### Stop All
```bash
docker-compose -f docker-compose-microservices.yml down
```

### Stop and Remove Volumes (CAUTION: Deletes all data)
```bash
docker-compose -f docker-compose-microservices.yml down -v
```

## 🔧 Rebuild After Code Changes

### Rebuild All Services
```bash
docker-compose -f docker-compose-microservices.yml build
docker-compose -f docker-compose-microservices.yml up -d
```

### Rebuild Specific Service
```bash
docker-compose -f docker-compose-microservices.yml build authorization-service
docker-compose -f docker-compose-microservices.yml up -d authorization-service
```

### Rebuild Without Cache (Clean Build)
```bash
docker-compose -f docker-compose-microservices.yml build --no-cache
docker-compose -f docker-compose-microservices.yml up -d
```

## 📈 Performance Monitoring

### Check Container Resource Usage
```bash
docker stats
```

### Check Container Status
```bash
docker-compose -f docker-compose-microservices.yml ps
```

### Check Disk Usage
```bash
docker system df
```

## 🐛 Troubleshooting

### Service Won't Start
```bash
# Check logs for errors
docker logs vsa-authorization-service --tail=100

# Check if port is already in use
lsof -i :8082

# Remove and recreate container
docker-compose -f docker-compose-microservices.yml rm -f authorization-service
docker-compose -f docker-compose-microservices.yml up -d authorization-service
```

### Database Connection Issues
```bash
# Check PostgreSQL logs
docker logs vsa-postgres --tail=50

# Verify PostgreSQL is healthy
docker exec vsa-postgres pg_isready -U payment_user

# Test connection
docker exec vsa-postgres psql -U payment_user -d payment_gateway_db -c "SELECT 1"
```

### Kafka Not Receiving Events
```bash
# Check Kafka logs
docker logs vsa-kafka --tail=50

# Check Zookeeper logs
docker logs vsa-zookeeper --tail=50

# Verify topics exist
docker exec -it vsa-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Describe topic
docker exec -it vsa-kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic payment-events
```

### Tests Failing
```bash
# Verify all services are healthy
for port in 8081 8082 8083 8084 8085; do
  curl -s http://localhost:$port/actuator/health | jq -r '.status'
done

# Check if unique IDs are being generated
./test-microservices.sh 2>&1 | grep "Customer ID:"

# Run single scenario
# (Edit test-microservices.sh to comment out other scenarios)
```

### Clean Slate (Nuclear Option)
```bash
# Stop everything
docker-compose -f docker-compose-microservices.yml down -v

# Remove all images
docker-compose -f docker-compose-microservices.yml down --rmi all

# Clean build and start
docker-compose -f docker-compose-microservices.yml build --no-cache
docker-compose -f docker-compose-microservices.yml up -d

# Wait for initialization
sleep 30

# Run tests
./test-microservices.sh
```

## 📚 Documentation

- **Testing Guide**: `docs/TESTING-GUIDE.md`
- **Phase 7 Complete**: `docs/PHASE-7-COMPLETE.md`
- **Test Results**: `docs/PHASE-7-TEST-RESULTS.md`
- **This Guide**: `docs/QUICK-REFERENCE.md`

## 🎯 Success Indicators

### All Systems Operational
```bash
# Run this one-liner to check everything
echo "=== Services ===" && \
for port in 8081 8082 8083 8084 8085; do 
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq -r '.status')"; 
done && \
echo "=== Kafka ===" && \
docker exec vsa-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null && \
echo "=== PostgreSQL ===" && \
docker exec vsa-postgres pg_isready -U payment_user
```

Expected output:
```
=== Services ===
Port 8081: UP
Port 8082: UP
Port 8083: UP
Port 8084: UP
Port 8085: UP
=== Kafka ===
__consumer_offsets
payment-events
=== PostgreSQL ===
/var/run/postgresql:5432 - accepting connections
```

If you see this, you're ready to test! 🎉

## 🆘 Getting Help

1. Check the logs: `docker logs vsa-<service-name> --tail=100`
2. Review the documentation: `docs/TESTING-GUIDE.md`
3. Verify service health: `curl http://localhost:808X/actuator/health`
4. Check Kafka events: See "Monitor Kafka Events" section above
5. Clean restart: See "Clean Slate" section above

---

**Last Updated**: November 4, 2025  
**Phase**: 7 - Docker Deployment  
**Status**: Complete ✅
