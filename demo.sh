#!/bin/bash

# Payment Gateway VSA Demo - Quick Start Script
# This script demonstrates the complete payment gateway flow

set -e

echo "🚀 Payment Gateway VSA Demo - Quick Start"
echo "=========================================="

# Check prerequisites
echo "📋 Checking prerequisites..."
command -v java >/dev/null 2>&1 || { echo "❌ Java is required but not installed. Aborting." >&2; exit 1; }

# Check for Docker - try common locations
DOCKER_CMD=""
if command -v docker >/dev/null 2>&1; then
    DOCKER_CMD="docker"
elif [ -f "/usr/local/bin/docker" ]; then
    DOCKER_CMD="/usr/local/bin/docker"
    echo "ℹ️  Found Docker at /usr/local/bin/docker"
elif [ -f "/Applications/Docker.app/Contents/Resources/bin/docker" ]; then
    DOCKER_CMD="/Applications/Docker.app/Contents/Resources/bin/docker"
    echo "ℹ️  Found Docker in Docker.app"
else
    echo "❌ Docker is required but not found. Please install Docker Desktop. Aborting." >&2
    exit 1
fi

command -v curl >/dev/null 2>&1 || { echo "❌ curl is required but not installed. Aborting." >&2; exit 1; }

echo "✅ Prerequisites check passed"

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for $service_name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s $url > /dev/null 2>&1; then
            echo "✅ $service_name is ready!"
            return 0
        fi
        echo "   Attempt $attempt/$max_attempts - waiting for $service_name..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name failed to start within expected time"
    return 1
}

# Function to check if a port is listening
check_port() {
    local service_name=$1
    local port=$2
    local max_attempts=30
    local attempt=1
    
    echo "⏳ Waiting for $service_name to be ready on port $port..."
    while [ $attempt -le $max_attempts ]; do
        if nc -z localhost $port 2>/dev/null; then
            echo "✅ $service_name is ready!"
            return 0
        fi
        echo "   Attempt $attempt/$max_attempts - waiting for $service_name..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo "❌ $service_name failed to start within expected time"
    return 1
}

# Start infrastructure services
echo "🐳 Starting infrastructure services..."
$DOCKER_CMD compose up -d postgres kafka zookeeper

# Wait for services to be ready
check_port "PostgreSQL" 5433 || {
    echo "Note: PostgreSQL check failed, but continuing..."
}
check_port "Kafka" 9092 || {
    echo "Note: Kafka check failed, but continuing..."
}

# Build the application
echo "🔨 Building the application..."
./mvnw clean package -DskipTests -q

# Start the application
echo "🎯 Starting Payment Gateway application..."
java -jar gateway-api/target/*.jar &
APP_PID=$!

# Wait for application to be ready
wait_for_service "Payment Gateway" "http://localhost:8080/actuator/health"

echo "🎉 Payment Gateway is running!"
echo ""
echo "🔗 Available endpoints:"
echo "   Application: http://localhost:8080"
echo "   Health Check: http://localhost:8080/actuator/health"
echo "   Metrics: http://localhost:8080/actuator/metrics"
echo "   Kafka UI: http://localhost:8088"
echo "   Axon Server: http://localhost:8024"
echo ""

# Demo the complete payment flow
echo "🎮 Demonstrating complete payment flow..."
echo ""

# Step 1: Register a customer
echo "👤 Step 1: Registering a customer..."
CUSTOMER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1-555-123-4567",
    "address": "123 Main St, Anytown, USA"
  }')

CUSTOMER_ID=$(echo $CUSTOMER_RESPONSE | tr -d '"')
echo "   ✅ Customer registered with ID: $CUSTOMER_ID"

# Step 2: Add payment method
echo "💳 Step 2: Adding payment method..."
curl -s -X POST http://localhost:8080/api/customers/$CUSTOMER_ID/payment-methods \
  -H "Content-Type: application/json" \
  -d '{
    "cardNumber": "4111111111111111",
    "expiryMonth": "12",
    "expiryYear": "25",
    "cvv": "123",
    "cardHolderName": "John Doe",
    "isDefault": true
  }' > /dev/null

echo "   ✅ Payment method added successfully"

# Step 3: Verify customer setup
echo "🔍 Step 3: Verifying customer setup..."
CUSTOMER_DATA=$(curl -s http://localhost:8080/api/customers/$CUSTOMER_ID/with-payment-methods)
echo "   ✅ Customer data retrieved:"
echo "   📧 Email: $(echo $CUSTOMER_DATA | grep -o '"email":"[^"]*' | cut -d'"' -f4)"
echo "   💳 Payment methods: $(echo $CUSTOMER_DATA | grep -o '"maskedCardNumber":"[^"]*' | cut -d'"' -f4)"

# Step 4: Test different scenarios
echo ""
echo "🧪 Testing different scenarios..."

# Test duplicate customer registration
echo "   🔄 Testing duplicate customer prevention..."
DUPLICATE_RESPONSE=$(curl -s -w "%{http_code}" -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1-555-123-4567",
    "address": "123 Main St, Anytown, USA"
  }')

if [[ "$DUPLICATE_RESPONSE" == *"400"* ]]; then
    echo "   ✅ Duplicate customer prevention working correctly"
else
    echo "   ❌ Duplicate customer prevention failed"
fi

# Test customer search
echo "   🔍 Testing customer search..."
SEARCH_RESULTS=$(curl -s "http://localhost:8080/api/customers/search?name=John")
if [[ "$SEARCH_RESULTS" == *"$CUSTOMER_ID"* ]]; then
    echo "   ✅ Customer search working correctly"
else
    echo "   ❌ Customer search failed"
fi

echo ""
echo "📊 System Status:"
echo "   🏥 Health: $(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*' | cut -d'"' -f4)"
echo "   📈 Metrics available at: http://localhost:8080/actuator/prometheus"

echo ""
echo "🎉 Demo completed successfully!"
echo ""
echo "📚 Next steps:"
echo "   1. Explore the API endpoints"
echo "   2. Check Kafka messages at http://localhost:8088"
echo "   3. Monitor events in Axon Server at http://localhost:8024"
echo "   4. Review the code structure to understand VSA implementation"
echo ""
echo "🛑 To stop the demo:"
echo "   - Press Ctrl+C to stop this script"
echo "   - Run: docker-compose down"
echo "   - The application will be stopped automatically"

# Keep the script running and handle cleanup
cleanup() {
    echo ""
    echo "🧹 Cleaning up..."
    if [ ! -z "$APP_PID" ]; then
        kill $APP_PID 2>/dev/null || true
    fi
    $DOCKER_CMD compose down
    echo "✅ Cleanup completed"
}

trap cleanup EXIT

# Wait for user to stop
echo "⏳ Demo is running. Press Ctrl+C to stop..."
wait $APP_PID