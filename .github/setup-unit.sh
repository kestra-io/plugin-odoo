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
    COMPOSE_FILE="docker-compose-ci.yml"
else
    DC_CMD="docker-compose"
    COMPOSE_FILE="docker-compose-ci.yml"
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

# Verify demo database is accessible and set up test user
echo "⏳ Verifying demo database is accessible..."
timeout=60
elapsed=0
while ! curl -s -X POST \
    -H "Content-Type: application/json" \
    -d '{"jsonrpc":"2.0","method":"call","params":{"service":"common","method":"version","args":[]}}' \
    http://localhost:8069/jsonrpc 2>/dev/null | grep -q "server_version"; do
    if [ $elapsed -ge $timeout ]; then
        echo "❌ Demo database not accessible within ${timeout} seconds"
        exit 1
    fi
    sleep 5
    elapsed=$((elapsed + 5))
    echo "⏳ Still waiting for demo database... (${elapsed}/${timeout}s)"
done
echo "✅ Demo database is accessible"

# Create test user (test@demo.com) by updating the admin user's login
echo "👤 Setting up test user test@demo.com..."
python3 << 'PYTHON_EOF'
import xmlrpc.client
import sys

url = 'http://localhost:8069'
db = 'demo'

try:
    common = xmlrpc.client.ServerProxy(f'{url}/xmlrpc/2/common')
    uid = common.authenticate(db, 'admin', 'admin', {})

    if not uid:
        print('❌ Cannot authenticate as admin to demo database')
        sys.exit(1)

    models = xmlrpc.client.ServerProxy(f'{url}/xmlrpc/2/object')

    # Check if test@demo.com already exists
    existing = models.execute_kw(db, uid, 'admin', 'res.users', 'search_count',
                                  [[['login', '=', 'test@demo.com']]])
    if existing > 0:
        print('✅ User test@demo.com already exists')
        sys.exit(0)

    # Update admin user login to test@demo.com so tests can authenticate
    models.execute_kw(db, uid, 'admin', 'res.users', 'write', [[uid], {
        'login': 'test@demo.com',
        'email': 'test@demo.com',
        'name': 'Demo Admin',
    }])
    print('✅ Updated admin user login to test@demo.com')

except Exception as e:
    print(f'❌ Failed to set up test user: {e}')
    sys.exit(1)
PYTHON_EOF

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