#!/bin/bash

###############################################################################
# VSA Payment Gateway - Stop Microservices Script
#
# This script stops all running microservices and infrastructure.
#
# Usage:
#   ./stop-microservices.sh [OPTIONS]
#
# Options:
#   --clean    Also remove volumes (cleans database data)
#
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

CLEAN_VOLUMES=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --clean)
            CLEAN_VOLUMES=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Usage: $0 [--clean]"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║    VSA Payment Gateway - Stop Microservices                   ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "\n${YELLOW}Stopping all services...${NC}\n"

if [ "$CLEAN_VOLUMES" = true ]; then
    echo -e "${YELLOW}Also removing volumes (database data will be lost)...${NC}"
    docker-compose -f docker-compose-microservices.yml down -v
else
    docker-compose -f docker-compose-microservices.yml down
fi

echo -e "\n${GREEN}✓ All services stopped successfully${NC}\n"

# Show remaining containers (if any)
RUNNING=$(docker ps --filter "name=vsa-" --format "{{.Names}}" | wc -l)
if [ $RUNNING -gt 0 ]; then
    echo -e "${YELLOW}Warning: Some VSA containers are still running:${NC}"
    docker ps --filter "name=vsa-"
else
    echo -e "${GREEN}✓ No VSA containers running${NC}"
fi

echo ""
