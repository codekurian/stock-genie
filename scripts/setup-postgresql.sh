#!/bin/bash

# PostgreSQL Setup Script for Stock Genie
# This script sets up PostgreSQL database for production use

echo "🐘 PostgreSQL Setup for Stock Genie"
echo "===================================="
echo ""

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL is not installed. Installing..."
    
    # Detect OS and install PostgreSQL
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        if command -v brew &> /dev/null; then
            echo "📦 Installing PostgreSQL via Homebrew..."
            brew install postgresql@15
            brew services start postgresql@15
        else
            echo "❌ Homebrew not found. Please install PostgreSQL manually:"
            echo "   Visit: https://www.postgresql.org/download/macos/"
            exit 1
        fi
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        if command -v apt-get &> /dev/null; then
            echo "📦 Installing PostgreSQL via apt..."
            sudo apt-get update
            sudo apt-get install -y postgresql postgresql-contrib
            sudo systemctl start postgresql
            sudo systemctl enable postgresql
        elif command -v yum &> /dev/null; then
            echo "📦 Installing PostgreSQL via yum..."
            sudo yum install -y postgresql-server postgresql-contrib
            sudo postgresql-setup initdb
            sudo systemctl start postgresql
            sudo systemctl enable postgresql
        else
            echo "❌ Package manager not found. Please install PostgreSQL manually."
            exit 1
        fi
    else
        echo "❌ Unsupported OS. Please install PostgreSQL manually."
        exit 1
    fi
else
    echo "✅ PostgreSQL is already installed"
fi

echo ""
echo "🔧 Setting up Stock Genie database..."

# Create database and user
sudo -u postgres psql << EOF
-- Create database
CREATE DATABASE stockgenie;

-- Create user
CREATE USER stockgenie_user WITH PASSWORD 'stockgenie_password';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE stockgenie TO stockgenie_user;

-- Connect to the database and grant schema privileges
\c stockgenie;
GRANT ALL ON SCHEMA public TO stockgenie_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO stockgenie_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO stockgenie_user;

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO stockgenie_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO stockgenie_user;

\q
EOF

echo "✅ Database setup complete!"
echo ""
echo "📋 Database Configuration:"
echo "   Database: stockgenie"
echo "   User: stockgenie_user"
echo "   Password: stockgenie_password"
echo "   Host: localhost"
echo "   Port: 5432"
echo ""

echo "🔧 Environment Variables:"
echo "   export DB_HOST=localhost"
echo "   export DB_PORT=5432"
echo "   export DB_NAME=stockgenie"
echo "   export DB_USER=stockgenie_user"
echo "   export DB_PASSWORD=stockgenie_password"
echo ""

echo "🧪 Testing database connection..."
if psql -h localhost -U stockgenie_user -d stockgenie -c "SELECT version();" &> /dev/null; then
    echo "✅ Database connection successful!"
else
    echo "❌ Database connection failed. Please check your setup."
    exit 1
fi

echo ""
echo "🚀 PostgreSQL setup complete!"
echo "   Your Stock Genie application is ready to use PostgreSQL."
echo ""
echo "💡 Next steps:"
echo "   1. Update your application.yml with the new database credentials"
echo "   2. Run the application with PostgreSQL profile"
echo "   3. Test the database integration"
