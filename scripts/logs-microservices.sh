#!/bin/bash

###############################################################################
# VSA Payment Gateway - View Microservices Logs Script
#
# This script shows logs from microservices.
#
# Usage:
#   ./logs-microservices.sh [SERVICE_NAME]
#
# Examples:
#   ./logs-microservices.sh                    # Show all logs
#   ./logs-microservices.sh customer-service   # Show customer service logs only
#   ./logs-microservices.sh kafka              # Show Kafka logs
#
# Available services:
#   - postgres
#   - zookeeper
#   - kafka
#   - customer-service
#   - authorization-service
#   - processing-service
#   - settlement-service
#   - orchestration-service
#
###############################################################################

set -e  # Exit on error

# Colors for output
BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

SERVICE_NAME=${1:-}

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║    VSA Payment Gateway - Microservices Logs                   ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

if [ -z "$SERVICE_NAME" ]; then
    echo -e "${GREEN}Showing logs for all services (Ctrl+C to exit)...${NC}\n"
    docker-compose -f docker-compose-microservices.yml logs -f --tail=100
else
    echo -e "${GREEN}Showing logs for: $SERVICE_NAME (Ctrl+C to exit)...${NC}\n"
    docker-compose -f docker-compose-microservices.yml logs -f --tail=100 $SERVICE_NAME
fi
