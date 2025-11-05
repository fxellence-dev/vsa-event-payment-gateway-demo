#!/bin/bash

###############################################################################
# VSA Payment Gateway - Monitor Kafka Events Script
#
# This script monitors events flowing through Kafka event bus.
#
# Usage:
#   ./monitor-kafka-events.sh
#
# What it does:
#   - Connects to Kafka container
#   - Subscribes to 'payment-events' topic
#   - Shows all events in real-time
#   - Displays events from the beginning of the topic
#
###############################################################################

set -e  # Exit on error

# Colors for output
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║    VSA Payment Gateway - Kafka Event Monitor                  ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${YELLOW}Checking if Kafka is running...${NC}"

if ! docker ps | grep -q vsa-kafka; then
    echo -e "${RED}Error: Kafka container is not running.${NC}"
    echo -e "${YELLOW}Please start microservices first: ./start-microservices.sh${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Kafka is running${NC}\n"
echo -e "${GREEN}Monitoring events on topic: payment-events${NC}"
echo -e "${YELLOW}Press Ctrl+C to exit${NC}\n"
echo -e "${BLUE}════════════════════════════════════════════════════════════════${NC}\n"

# Monitor Kafka events
docker exec -it vsa-kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic payment-events \
    --from-beginning \
    --property print.timestamp=true \
    --property print.key=true \
    --property key.separator=" | "
