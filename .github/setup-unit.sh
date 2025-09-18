#!/bin/bash

# Setup script for Kestra Odoo Plugin Unit Tests
# This script sets up a local Odoo instance for testing both locally and in CI

set -e

echo "🐳 Setting up Odoo for unit tests..."

# Check if Docker and Docker Compose are available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not in PATH"
    exit 1
fi

# Check for Docker Compose (both v1 and v2)
if ! command -v docker-compose &> /dev/null && ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose is not installed or not in PATH"
    exit 1
fi

# Use docker compose (v2) if available, otherwise fall back to docker-compose (v1)
if command -v docker compose &> /dev/null; then
    DC_CMD="docker compose"
    COMPOSE_FILE="docker-compose.yml"
else
    DC_CMD="docker-compose"
    COMPOSE_FILE="odoo-docker-compose.yml"
fi

# Stop and remove any existing containers
echo "🧹 Cleaning up existing containers..."
$DC_CMD -f $COMPOSE_FILE down -v --remove-orphans || true

# Start Odoo and PostgreSQL containers
echo "🚀 Starting Odoo and PostgreSQL containers..."
$DC_CMD -f $COMPOSE_FILE up -d --build

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
timeout=60
elapsed=0
while ! $DC_CMD -f $COMPOSE_FILE exec -T postgres pg_isready -U odoo -d postgres; do
    if [ $elapsed -ge $timeout ]; then
        echo "❌ PostgreSQL failed to start within ${timeout} seconds"
        $DC_CMD -f $COMPOSE_FILE logs postgres
        exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
done

echo "✅ PostgreSQL is ready"

# Wait for Odoo to be ready
echo "⏳ Waiting for Odoo to be ready..."
timeout=120
elapsed=0
while ! curl -f -s http://localhost:8069/web/database/selector &> /dev/null; do
    if [ $elapsed -ge $timeout ]; then
        echo "❌ Odoo failed to start within ${timeout} seconds"
        $DC_CMD -f $COMPOSE_FILE logs odoo
        exit 1
    fi
    sleep 5
    elapsed=$((elapsed + 5))
    echo "⏳ Still waiting for Odoo... (${elapsed}/${timeout}s)"
done

echo "✅ Odoo is ready"

# Create the demo database via Odoo's web interface using curl
echo "🗄️ Creating demo database..."

# First, check if database already exists
if curl -s -X POST -d "name=demo" http://localhost:8069/web/database/list | grep -q "demo"; then
    echo "✅ Demo database already exists"
else
    # Create database using Odoo's database manager
    # This simulates the database creation form submission
    curl -s -X POST \
        -d "master_pwd=admin" \
        -d "name=demo" \
        -d "login=test@demo.com" \
        -d "password=admin" \
        -d "phone=" \
        -d "lang=en_US" \
        -d "country_code=US" \
        -d "demo=true" \
        http://localhost:8069/web/database/create

    # Wait a bit for database creation to complete
    echo "⏳ Waiting for database creation to complete..."
    sleep 30

    # Verify database was created
    if curl -s -X POST -d "name=demo" http://localhost:8069/web/database/list | grep -q "demo"; then
        echo "✅ Demo database created successfully"
    else
        echo "⚠️ Database creation may still be in progress, tests will retry as needed"
    fi
fi

# Show status
echo "📊 Container status:"
$DC_CMD -f $COMPOSE_FILE ps

echo ""
echo "🎉 Setup complete!"
echo ""
echo "📋 Connection details:"
echo "  URL: http://localhost:8069"
echo "  Database: demo"
echo "  Username: test@demo.com"
echo "  Password: admin"
echo ""
echo "🧪 You can now run tests with:"
echo "  ./gradlew test"
echo "  ODOO_INTEGRATION_TESTS=true ./gradlew test"
echo ""
echo "🛑 To stop the services:"
echo "  $DC_CMD -f $COMPOSE_FILE down"