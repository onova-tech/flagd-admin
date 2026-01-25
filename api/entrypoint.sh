#!/bin/sh
set -e

echo "🚀 Flagd Admin API Container Starting..."

# Validate or generate JWT secret
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

# Generate password hash if not provided
if [ -z "$FLAGD_ADMIN_PASSWORD_HASH" ]; then
    echo "⚠️  FLAGD_ADMIN_PASSWORD_HASH not set, generating hash for 'pass'..."
    export FLAGD_ADMIN_PASSWORD_HASH=$(python3 -c "
import bcrypt
password = 'pass'
salt = bcrypt.gensalt()
hashed = bcrypt.hashpw(password.encode('utf-8'), salt)
print(hashed.decode('utf-8'))
" 2>/dev/null || echo '$2a$12$Q7nr0df4IWXiz1vSByznyOJ3LdoA0MXnwT/gqkKYHa6f2C6X8go0m')
    echo "✅ Generated password hash for '$FLAGD_ADMIN_USERNAME'"
fi

# Show configuration
echo "📋 Container Configuration:"
echo "   • Admin Username: $FLAGD_ADMIN_USERNAME"
echo "   • JWT Secret: ${FLAGD_JWT_SECRET:0:10}..."
echo "   • Access Token Expiration: ${FLAGD_ACCESS_TOKEN_EXPIRATION}ms"
echo "   • Refresh Token Expiration: ${FLAGD_REFRESH_TOKEN_EXPIRATION}ms"

exec "$@"