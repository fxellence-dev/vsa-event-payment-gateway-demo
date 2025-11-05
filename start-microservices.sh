#!/bin/bash

###############################################################################
# VSA Payment Gateway - Microservices Deployment Script
#
# This script builds and starts all microservices in Docker containers.
#
# Usage:
#   ./start-microservices.sh
#
# What it does:
#   1. Builds all Docker images for microservices
#   2. Starts infrastructure (PostgreSQL, Zookeeper, Kafka)
#   3. Waits for infrastructure to be ready
#   4. Starts all microservices
#   5. Shows logs and health status
#
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                                                                ║"
echo "║    VSA Payment Gateway - Microservices Deployment             ║"
echo "║                                                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Function to print section headers
print_header() {
    echo -e "\n${BLUE}═══ $1 ═══${NC}\n"
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running. Please start Docker and try again.${NC}"
        exit 1
    fi
}

# Function to wait for a service to be healthy
wait_for_service() {
    local service_name=$1
    local max_attempts=60
    local attempt=1
    
    echo -e "${YELLOW}Waiting for $service_name to be healthy...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose -f docker-compose-microservices.yml ps | grep $service_name | grep -q "healthy"; then
            echo -e "${GREEN}✓ $service_name is healthy${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}✗ $service_name failed to become healthy after $((max_attempts * 2)) seconds${NC}"
    return 1
}

# Main deployment process
main() {
    print_header "Step 1: Checking Prerequisites"
    check_docker
    echo -e "${GREEN}✓ Docker is running${NC}"
    
    print_header "Step 2: Stopping Existing Containers"
    docker-compose -f docker-compose-microservices.yml down >/dev/null 2>&1 || true
    
    # Remove any conflicting buildkit containers
    docker rm -f buildx_buildkit_multiarch0 >/dev/null 2>&1 || true
    
    echo -e "${GREEN}✓ Existing containers stopped${NC}"
    
    print_header "Step 3: Building Docker Images"
    echo -e "${YELLOW}This may take several minutes on first run...${NC}"
    docker-compose -f docker-compose-microservices.yml build
    echo -e "${GREEN}✓ All images built successfully${NC}"
    
    print_header "Step 4: Starting Infrastructure Services"
    docker-compose -f docker-compose-microservices.yml up -d postgres zookeeper kafka
    
    # Wait for infrastructure
    wait_for_service "postgres"
    wait_for_service "zookeeper"
    wait_for_service "kafka"
    
    print_header "Step 5: Starting Microservices"
    docker-compose -f docker-compose-microservices.yml up -d \
        customer-service \
        authorization-service \
        processing-service \
        settlement-service \
        orchestration-service
    
    echo -e "${GREEN}✓ All microservices started${NC}"
    
    print_header "Step 6: Waiting for Services to be Ready"
    echo -e "${YELLOW}Waiting 30 seconds for services to initialize...${NC}"
    sleep 30
    
    print_header "Deployment Status"
    docker-compose -f docker-compose-microservices.yml ps
    
    print_header "Service URLs"
    echo -e "${GREEN}Customer Service:      http://localhost:8081${NC}"
    echo -e "${GREEN}Authorization Service: http://localhost:8082${NC}"
    echo -e "${GREEN}Processing Service:    http://localhost:8083${NC}"
    echo -e "${GREEN}Settlement Service:    http://localhost:8084${NC}"
    echo -e "${GREEN}Orchestration Service: http://localhost:8085${NC}"
    
    print_header "Health Check Endpoints"
    echo -e "${BLUE}Customer:      http://localhost:8081/actuator/health${NC}"
    echo -e "${BLUE}Authorization: http://localhost:8082/actuator/health${NC}"
    echo -e "${BLUE}Processing:    http://localhost:8083/actuator/health${NC}"
    echo -e "${BLUE}Settlement:    http://localhost:8084/actuator/health${NC}"
    echo -e "${BLUE}Orchestration: http://localhost:8085/actuator/health${NC}"
    
    print_header "Useful Commands"
    echo -e "${YELLOW}View all logs:${NC}"
    echo "  docker-compose -f docker-compose-microservices.yml logs -f"
    echo ""
    echo -e "${YELLOW}View specific service logs:${NC}"
    echo "  docker-compose -f docker-compose-microservices.yml logs -f customer-service"
    echo ""
    echo -e "${YELLOW}Monitor Kafka events:${NC}"
    echo "  docker exec -it vsa-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic payment-events --from-beginning"
    echo ""
    echo -e "${YELLOW}Stop all services:${NC}"
    echo "  docker-compose -f docker-compose-microservices.yml down"
    echo ""
    echo -e "${YELLOW}Restart a service:${NC}"
    echo "  docker-compose -f docker-compose-microservices.yml restart customer-service"
    
    echo -e "\n${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  Microservices deployment complete!                           ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}\n"
}

# Run main function
main
