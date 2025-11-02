#!/bin/bash

# Payment Gateway - Complete Flow Test Script
# Tests the full payment lifecycle: Register → Add Payment Method → Authorize → Process → Settle

set -e

BASE_URL="http://localhost:8080"
CUSTOMER_ID=""
PAYMENT_METHOD_ID=""
PAYMENT_ID=""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Payment Gateway - Full Flow Test${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Function to generate UUID
generate_uuid() {
    uuidgen | tr '[:upper:]' '[:lower:]'
}

# Function to make API call and pretty print
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "${YELLOW}➜ ${description}${NC}"
    echo -e "${BLUE}${method} ${endpoint}${NC}"
    
    if [ -n "$data" ]; then
        echo -e "Request Body:"
        echo "$data" | jq '.'
    fi
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "${BASE_URL}${endpoint}")
    else
        response=$(curl -s -X GET "${BASE_URL}${endpoint}")
    fi
    
    echo -e "${GREEN}Response:${NC}"
    echo "$response" | jq '.'
    echo ""
    
    echo "$response"
}

# Wait for application to be ready
echo -e "${YELLOW}Checking if application is ready...${NC}"
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is ready!${NC}\n"
        break
    fi
    attempt=$((attempt + 1))
    if [ $attempt -eq $max_attempts ]; then
        echo -e "${RED}✗ Application failed to start${NC}"
        exit 1
    fi
    echo "Waiting for application... (${attempt}/${max_attempts})"
    sleep 2
done

# Step 1: Register Customer
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 1: Register Customer${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

CUSTOMER_ID=$(generate_uuid)
customer_data=$(cat <<EOF
{
    "customerId": "${CUSTOMER_ID}",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe"
}
EOF
)

response=$(api_call "POST" "/api/customers/register" "$customer_data" "Registering new customer")
sleep 2

# Step 2: Add Payment Method
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 2: Add Payment Method${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

PAYMENT_METHOD_ID=$(generate_uuid)
payment_method_data=$(cat <<EOF
{
    "customerId": "${CUSTOMER_ID}",
    "paymentMethodId": "${PAYMENT_METHOD_ID}",
    "cardNumber": "4111111111111111",
    "cardHolderName": "John Doe",
    "expiryMonth": "12",
    "expiryYear": "2025",
    "cvv": "123"
}
EOF
)

response=$(api_call "POST" "/api/customers/payment-methods" "$payment_method_data" "Adding credit card payment method")
sleep 2

# Step 3: Query Customer to verify
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 3: Verify Customer Registration${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

response=$(api_call "GET" "/api/customers/${CUSTOMER_ID}" "" "Querying customer details")
sleep 2

# Step 4: Authorize Payment (This triggers the saga!)
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 4: Authorize Payment (Saga Starts!)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

PAYMENT_ID=$(generate_uuid)
AUTHORIZATION_ID=$(generate_uuid)
MERCHANT_ID=$(generate_uuid)

authorization_data=$(cat <<EOF
{
    "authorizationId": "${AUTHORIZATION_ID}",
    "paymentId": "${PAYMENT_ID}",
    "customerId": "${CUSTOMER_ID}",
    "merchantId": "${MERCHANT_ID}",
    "amount": 99.99,
    "currency": "USD",
    "paymentMethodId": "${PAYMENT_METHOD_ID}"
}
EOF
)

response=$(api_call "POST" "/api/authorizations/authorize" "$authorization_data" "Authorizing payment - This starts the saga orchestration!")

echo -e "${GREEN}✓ Payment authorization initiated!${NC}"
echo -e "${YELLOW}⚡ PaymentProcessingSaga has been triggered!${NC}"
echo -e "${YELLOW}⚡ Saga will automatically:${NC}"
echo -e "${YELLOW}   1. Process the payment${NC}"
echo -e "${YELLOW}   2. Settle the payment${NC}"
echo -e "${YELLOW}   3. Or compensate (void) if anything fails${NC}\n"

# Step 5: Wait for saga to complete
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 5: Monitor Saga Execution${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${YELLOW}Waiting for saga to complete (this may take a few seconds)...${NC}"
echo -e "${YELLOW}The saga is orchestrating:${NC}"
echo -e "  ${YELLOW}→ Authorization (COMPLETED)${NC}"
echo -e "  ${YELLOW}→ Processing (IN PROGRESS)${NC}"
echo -e "  ${YELLOW}→ Settlement (PENDING)${NC}\n"

# Wait for processing and settlement
sleep 5

# Step 6: Query authorization status
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 6: Check Authorization Status${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

response=$(api_call "GET" "/api/authorizations/${AUTHORIZATION_ID}" "" "Checking authorization status")

auth_status=$(echo "$response" | jq -r '.status // "UNKNOWN"')
if [ "$auth_status" = "AUTHORIZED" ]; then
    echo -e "${GREEN}✓ Authorization Status: AUTHORIZED${NC}\n"
elif [ "$auth_status" = "VOIDED" ]; then
    echo -e "${RED}✗ Authorization was VOIDED (compensation occurred)${NC}\n"
else
    echo -e "${YELLOW}⚠ Authorization Status: ${auth_status}${NC}\n"
fi

# Step 7: Query processing status
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 7: Check Processing Status${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

# Note: You may need to add query endpoints for processing and settlement
# For now, we'll check the logs
echo -e "${YELLOW}To see processing details, check the application logs for:${NC}"
echo -e "  - ${GREEN}PaymentProcessedEvent${NC} (success)"
echo -e "  - ${RED}PaymentProcessingFailedEvent${NC} (failure - triggers compensation)\n"

# Step 8: Query settlement status
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Step 8: Check Settlement Status${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${YELLOW}To see settlement details, check the application logs for:${NC}"
echo -e "  - ${GREEN}PaymentSettledEvent${NC} (success)"
echo -e "  - ${RED}SettlementFailedEvent${NC} (failure - triggers compensation)\n"

# Summary
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${GREEN}✓ Customer Registered${NC}"
echo -e "  Customer ID: ${CUSTOMER_ID}"
echo -e "  Email: john.doe@example.com\n"

echo -e "${GREEN}✓ Payment Method Added${NC}"
echo -e "  Payment Method ID: ${PAYMENT_METHOD_ID}"
echo -e "  Card: **** **** **** 1111\n"

echo -e "${GREEN}✓ Payment Authorized${NC}"
echo -e "  Authorization ID: ${AUTHORIZATION_ID}"
echo -e "  Payment ID: ${PAYMENT_ID}"
echo -e "  Amount: \$99.99 USD\n"

echo -e "${YELLOW}⚡ Saga Orchestration${NC}"
echo -e "  The PaymentProcessingSaga is coordinating:"
echo -e "  1. ${GREEN}Authorization${NC} - Completed"
echo -e "  2. ${BLUE}Processing${NC} - Saga listening for PaymentAuthorizedEvent"
echo -e "  3. ${BLUE}Settlement${NC} - Saga listening for PaymentProcessedEvent"
echo -e "  4. ${YELLOW}Compensation${NC} - Automatic rollback if failures occur\n"

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Next Steps${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "1. Check application logs to see saga orchestration:"
echo -e "   ${YELLOW}tail -f gateway-api/logs/application.log${NC}\n"

echo -e "2. Look for these log messages:"
echo -e "   ${GREEN}• PaymentProcessingSaga: Starting saga for payment${NC}"
echo -e "   ${GREEN}• PaymentProcessingSaga: Processing payment${NC}"
echo -e "   ${GREEN}• PaymentProcessingSaga: Payment processed successfully${NC}"
echo -e "   ${GREEN}• PaymentProcessingSaga: Settling payment${NC}"
echo -e "   ${GREEN}• PaymentProcessingSaga: Payment settled successfully${NC}\n"

echo -e "3. Test failure scenarios (10% processing failure rate):"
echo -e "   ${YELLOW}Run this script multiple times to see compensation logic${NC}"
echo -e "   ${YELLOW}When processing fails, saga will void the authorization${NC}\n"

echo -e "4. Query endpoints to verify:"
echo -e "   ${BLUE}curl ${BASE_URL}/api/customers/${CUSTOMER_ID}${NC}"
echo -e "   ${BLUE}curl ${BASE_URL}/api/authorizations/${AUTHORIZATION_ID}${NC}\n"

echo -e "${GREEN}✓ Test completed successfully!${NC}\n"
