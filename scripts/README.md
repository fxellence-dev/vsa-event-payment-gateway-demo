# Utility Scripts

This directory contains utility scripts for development, testing, and monitoring.

## Available Scripts

### Testing Scripts
- **`quick-test.sh`** - Quick smoke test of basic functionality
- **`test-microservices.sh`** - Basic microservices test suite
- **`test-payment-flow.sh`** - Payment flow integration test

### Monitoring Scripts
- **`monitor-kafka-events.sh`** - Real-time Kafka event monitoring
- **`logs-microservices.sh`** - View logs from all microservices
- **`check-postgres.sh`** - PostgreSQL database health check

### Demo Scripts
- **`demo.sh`** - Interactive demo of the payment gateway

## Usage

All scripts should be run from the project root directory:

```bash
cd /path/to/VSA-Demo
./scripts/script-name.sh
```

## Notes

- Main operational scripts (start, stop, test) are kept in the root directory for easy access
- These utility scripts are for advanced usage, debugging, and development
