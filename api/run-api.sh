#!/bin/bash
set -e

echo "🚀 Starting Flagd Admin API..."

# Ensure database file exists
[ -f src/main/resources/app.db ] || touch src/main/resources/app.db

# Auto-generate JWT secret if not provided
if [ -z "$FLAGD_JWT_SECRET" ]; then
    echo "⚠️  FLAGD_JWT_SECRET not set, generating secure random secret..."
    export FLAGD_JWT_SECRET=$(openssl rand -base64 32)
    echo "✅ Generated JWT secret: ${FLAGD_JWT_SECRET:0:10}..."
fi

# Set default admin username if not provided
if [ -z "$FLAGD_ADMIN_USERNAME" ]; then
    echo "⚠️  FLAGD_ADMIN_USERNAME not set, using default: admin"
    export FLAGD_ADMIN_USERNAME="admin"
fi

# Auto-generate password hash for "pass" if not provided
if [ -z "$FLAGD_ADMIN_PASSWORD_HASH" ]; then
    echo "⚠️  FLAGD_ADMIN_PASSWORD_HASH not set, generating hash for default password: pass"
    export FLAGD_ADMIN_PASSWORD_HASH=$(python3 -c "
import bcrypt
password = 'pass'
salt = bcrypt.gensalt()
hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
print(hashed.decode('utf-8'))
" 2>/dev/null || echo '$2a$12$Q7nr0df4IWXiz1vSByznyOJ3LdoA0MXnwT/gqkKYHa6f2C6X8go0m')
    echo "✅ Generated password hash for '$FLAGD_ADMIN_USERNAME'"
fi

# Show configuration summary
echo "📋 Configuration Summary:"
echo "   • Auth Provider: ${FLAGD_AUTH_PROVIDER:-jwt}"
echo "   • Admin Username: $FLAGD_ADMIN_USERNAME"
echo "   • Access Token Expiration: ${FLAGD_ACCESS_TOKEN_EXPIRATION:-900000}ms"
echo "   • Refresh Token Expiration: ${FLAGD_REFRESH_TOKEN_EXPIRATION:-604800000}ms"
echo ""

# Start application
echo "🔄 Starting Spring Boot application..."
./gradlew bootRun
