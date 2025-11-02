#!/bin/bash

echo "🔍 PostgreSQL Services Analysis"
echo "==============================="
echo ""

echo "📊 Current PostgreSQL processes:"
ps aux | grep postgres | grep -v grep || echo "No PostgreSQL processes found via ps"
echo ""

echo "📡 Network connections on PostgreSQL ports:"
echo "Port 5432 (standard):"
netstat -an | grep 5432 || echo "Nothing listening on 5432"
echo ""
echo "Port 5433 (our Docker):"
netstat -an | grep 5433 || echo "Nothing listening on 5433"
echo ""

echo "🐳 Docker containers:"
docker compose ps postgres 2>/dev/null || echo "No Docker PostgreSQL container found"
echo ""

echo "🍺 Homebrew services:"
if command -v brew >/dev/null 2>&1; then
    # Try to run brew services without requiring Xcode license
    brew services list 2>/dev/null | grep postgres || echo "No Homebrew PostgreSQL services found (or Xcode license needed)"
else
    echo "Homebrew not found"
fi
echo ""

echo "💡 Recommendations:"
echo "1. Your VSA demo is now using PostgreSQL on port 5433"
echo "2. Your existing PostgreSQL on port 5432 appears to be running"
echo "3. Both can coexist without conflicts"
echo "4. To connect to demo database: localhost:5433"
echo "5. To connect to existing database: localhost:5432"