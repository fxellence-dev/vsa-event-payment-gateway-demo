# VSA Payment Gateway - One Command Start

## 🚀 TL;DR - Start Everything

```bash
./demo.sh
```

That's it! 🎉

## What This Does

The single command above will:
1. Check prerequisites
2. Start all infrastructure (Docker containers)
3. Build the application
4. Start the application
5. Run a complete demo flow
6. Keep running until you press `Ctrl+C`

## Alternative: Step-by-Step Manual Control

### Start Infrastructure Only
```bash
docker compose up -d
```

### Build & Run Application
```bash
./mvnw clean package -DskipTests
java -jar gateway-api/target/gateway-api-1.0.0-SNAPSHOT.jar
```

### Stop Everything
```bash
docker compose down
```

## Quick Health Check

```bash
# Check if everything is running
curl http://localhost:8080/actuator/health | jq '.'

# Test Customer API
curl http://localhost:8080/api/customers
```

## Key URLs

- **Application**: http://localhost:8080
- **Health**: http://localhost:8080/actuator/health
- **Kafka UI**: http://localhost:8088
- **PostgreSQL**: localhost:5433

---

For detailed documentation, see **QUICK-START.md**
