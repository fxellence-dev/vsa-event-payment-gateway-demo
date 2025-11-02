# VSA Payment Gateway - Quick Start Guide

## 🚀 How to Start the Application

### Option 1: Using the Demo Script (Recommended)

The `demo.sh` script automates everything for you:

```bash
# 1. Make the script executable (first time only)
chmod +x demo.sh

# 2. Run the demo script
./demo.sh
```

**What the script does:**
1. ✅ Checks prerequisites (Java, Docker, curl)
2. ✅ Starts all infrastructure (PostgreSQL, Kafka, Zookeeper)
3. ✅ Builds the application
4. ✅ Starts the Payment Gateway application
5. ✅ Runs a complete demo flow:
   - Registers a customer
   - Adds payment method
   - Verifies customer setup
   - Tests duplicate prevention
   - Tests customer search

**To stop the demo:**
```bash
# Press Ctrl+C
# The script will automatically clean up and stop all services
```

---

### Option 2: Manual Start (Step by Step)

If you prefer to run each step manually:

#### Step 1: Start Infrastructure
```bash
docker compose up -d
```

#### Step 2: Verify Infrastructure is Running
```bash
# Check Docker containers
docker ps

# Test PostgreSQL
docker exec -it vsa-demo-postgres-1 psql -U paymentgateway -d paymentgateway -c "SELECT 1;"

# Test Kafka (should show broker)
docker logs vsa-demo-kafka-1 --tail 20
```

#### Step 3: Build the Application
```bash
./mvnw clean package -DskipTests
```

#### Step 4: Start the Application
```bash
java -jar gateway-api/target/gateway-api-1.0.0-SNAPSHOT.jar
```

#### Step 5: Test the Application
```bash
# In a new terminal window:

# Check health
curl http://localhost:8080/actuator/health | jq '.'

# List customers
curl http://localhost:8080/api/customers | jq '.'

# Register customer
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1-555-123-4567",
    "address": "123 Main St, Anytown, USA"
  }'
```

---
```

---

### Option 3: Using Maven Spring Boot Plugin

You can also run the application using Maven:

```bash
# Start infrastructure first
docker compose up -d

# Run the application with Maven
./mvnw spring-boot:run -pl gateway-api
```

---

## 📊 Monitoring & Management

### Infrastructure Services

| Service | URL | Description |
|---------|-----|-------------|
| **Kafka UI** | http://localhost:8088 | Kafka topics and messages |
| **PostgreSQL** | localhost:5433 | Database (user: postgres, db: payment_gateway) |

### Application Endpoints

| Endpoint | URL | Description |
|----------|-----|-------------|
| **Health Check** | http://localhost:8080/actuator/health | Application health status |
| **Metrics** | http://localhost:8080/actuator/metrics | Application metrics |
| **Customer API** | http://localhost:8080/api/customers | Customer management |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | API documentation (if enabled) |

---

## 🛑 How to Stop Everything

### If Using demo.sh:
```bash
# Press Ctrl+C (the script handles cleanup automatically)
```

### If Running Manually:
```bash
# 1. Stop the application (Ctrl+C or kill the process)
pkill -f gateway-api-1.0.0-SNAPSHOT.jar

# 2. Stop infrastructure
docker compose down

# 3. (Optional) Remove all data volumes
docker compose down -v
```

---

## 🔍 Troubleshooting

### Check What's Running
```bash
# Check Docker containers
docker ps

# Check application process
ps aux | grep gateway-api

# Check port 8080
lsof -i :8080
```

### View Logs
```bash
# Application logs (if running in background)
tail -f app.log

# Docker container logs
docker logs vsa-demo-postgres-1
docker logs vsa-demo-kafka-1
```

### Common Issues

**Issue: Port 8080 already in use**
```bash
# Find and kill the process
lsof -i :8080
kill -9 <PID>
```

**Issue: Kafka not starting**
```bash
# Check Kafka logs
docker logs vsa-demo-kafka-1

# Restart Kafka
docker compose restart kafka
```

**Issue: Database connection failed**
```bash
# Check PostgreSQL logs
docker logs vsa-demo-postgres-1

# Restart PostgreSQL
docker compose restart postgres
```

---

## 🎯 Quick Demo Commands

### Customer Management
```bash
# Register customer
CUSTOMER_ID=$(curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Jane Smith",
    "email": "jane.smith@example.com",
    "phoneNumber": "+1-555-987-6543",
    "address": "456 Oak Ave, Springfield, USA"
  }' | tr -d '"')

echo "Customer ID: $CUSTOMER_ID"

# Add payment method (use valid Luhn card number like 4532015112830366)
curl -X POST http://localhost:8080/api/customers/$CUSTOMER_ID/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4532015112830366",
    "expiryMonth": "12",
    "expiryYear": "26",
    "cvv": "123",
    "cardHolderName": "Jane Smith",
    "isDefault": true
  }'

# Get customer with payment methods
curl http://localhost:8080/api/customers/$CUSTOMER_ID/with-payment-methods | jq '.'

# Search customers
curl "http://localhost:8080/api/customers/search?name=Jane" | jq '.'

# List all customers
curl http://localhost:8080/api/customers | jq '.'
```

---

## 📚 Additional Resources

- **README.md** - Complete project documentation
- **VSA-DEMO-SUMMARY.md** - Architecture and implementation details
- **docker-compose.yml** - Infrastructure configuration
- **pom.xml** - Maven build configuration

---

## 🎉 Success Indicators

When everything is running correctly, you should see:

✅ 3 Docker containers running (postgres, kafka, zookeeper)
✅ Application responds on http://localhost:8080
✅ Health endpoint shows status UP
✅ Customer API returns `[]` (empty array) on first run
✅ Can register customers successfully
✅ Events are persisted to PostgreSQL (JPA-based event store)

---

## 📝 Important Notes

- **Event Store**: This application uses Axon Framework's JPA-based event store, not AxonServer
- **Card Validation**: Payment card numbers must pass Luhn algorithm validation
- **Valid Test Cards**: Use cards like `4532015112830366` (VISA) for testing
- **Database**: PostgreSQL stores both read models and event sourcing data
