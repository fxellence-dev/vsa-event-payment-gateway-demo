#!/bin/bash

# Quick Payment Flow Test - Simplified version

BASE_URL="http://localhost:8080"

echo "==================================="
echo "Quick Payment Gateway Test"
echo "==================================="
echo ""

# Generate UUIDs
CUSTOMER_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
PAYMENT_METHOD_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
PAYMENT_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
AUTHORIZATION_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
MERCHANT_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

echo "Generated IDs:"
echo "  Customer ID: $CUSTOMER_ID"
echo "  Payment Method ID: $PAYMENT_METHOD_ID"
echo "  Payment ID: $PAYMENT_ID"
echo "  Authorization ID: $AUTHORIZATION_ID"
echo ""

# Step 1: Register Customer
echo "Step 1: Registering customer..."
curl -s -X POST "${BASE_URL}/api/customers/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"${CUSTOMER_ID}\",
    \"email\": \"john.doe@example.com\",
    \"firstName\": \"John\",
    \"lastName\": \"Doe\"
  }" | jq '.'
echo ""
sleep 1

# Step 2: Add Payment Method
echo "Step 2: Adding payment method..."
curl -s -X POST "${BASE_URL}/api/customers/payment-methods" \
  -H "Content-Type: application/json" \
  -d "{
    \"customerId\": \"${CUSTOMER_ID}\",
    \"paymentMethodId\": \"${PAYMENT_METHOD_ID}\",
    \"cardNumber\": \"4111111111111111\",
    \"cardHolderName\": \"John Doe\",
    \"expiryMonth\": \"12\",
    \"expiryYear\": \"2025\",
    \"cvv\": \"123\"
  }" | jq '.'
echo ""
sleep 1

# Step 3: Authorize Payment (Triggers Saga!)
echo "Step 3: Authorizing payment (SAGA STARTS!)..."
curl -s -X POST "${BASE_URL}/api/authorizations/authorize" \
  -H "Content-Type: application/json" \
  -d "{
    \"authorizationId\": \"${AUTHORIZATION_ID}\",
    \"paymentId\": \"${PAYMENT_ID}\",
    \"customerId\": \"${CUSTOMER_ID}\",
    \"merchantId\": \"${MERCHANT_ID}\",
    \"amount\": 99.99,
    \"currency\": \"USD\",
    \"paymentMethodId\": \"${PAYMENT_METHOD_ID}\"
  }" | jq '.'
echo ""

echo "⚡ PaymentProcessingSaga has been triggered!"
echo "⚡ Saga will orchestrate: Authorization → Processing → Settlement"
echo ""

# Wait for saga to complete
echo "Waiting for saga orchestration (5 seconds)..."
sleep 5

# Step 4: Query Authorization
echo "Step 4: Checking authorization status..."
curl -s -X GET "${BASE_URL}/api/authorizations/${AUTHORIZATION_ID}" | jq '.'
echo ""

# Step 5: Query Customer
echo "Step 5: Verifying customer details..."
curl -s -X GET "${BASE_URL}/api/customers/${CUSTOMER_ID}" | jq '.'
echo ""

echo "==================================="
echo "Test Complete!"
echo "==================================="
echo ""
echo "Check application logs to see saga orchestration:"
echo "  tail -f app.log | grep -i saga"
echo ""
echo "Look for these events:"
echo "  - PaymentAuthorizedEvent"
echo "  - PaymentProcessedEvent (or PaymentProcessingFailedEvent)"
echo "  - PaymentSettledEvent (or SettlementFailedEvent)"
echo "  - AuthorizationVoidedEvent (if compensation occurred)"
echo ""
