#!/bin/sh
set -e

echo "🚀 Flagd Admin Container Starting..."

# Function to validate or generate JWT secret
generate_jwt_secret() {
    if [ -z "$FLAGD_JWT_SECRET" ]; then
        echo "⚠️  FLAGD_JWT_SECRET not set, generating secure random secret..."
        export FLAGD_JWT_SECRET=$(openssl rand -base64 32)
        echo "✅ Generated JWT secret: ${FLAGD_JWT_SECRET:0:10}..."
    else
        echo "✅ Using provided JWT secret: ${FLAGD_JWT_SECRET:0:10}..."
    fi
}

# Function to set default admin username
set_admin_username() {
    if [ -z "$FLAGD_ADMIN_USERNAME" ]; then
        echo "⚠️  FLAGD_ADMIN_USERNAME not set, using default: admin"
        export FLAGD_ADMIN_USERNAME="admin"
    else
        echo "✅ Using admin username: $FLAGD_ADMIN_USERNAME"
    fi
}

# Function to generate password hash
generate_password_hash() {
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
    else
        echo "✅ Using provided password hash"
    fi
}

# Function to setup SSL configuration
setup_ssl_config() {
    if [ -d "/etc/letsencrypt/live" ] && [ -f "/etc/letsencrypt/live/$(hostname)/fullchain.pem" ]; then
        DOMAIN=$(basename /etc/letsencrypt/live/* 2>/dev/null | head -1)
        if [ ! -z "$DOMAIN" ] && [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
            echo "🔒 Setting up SSL configuration for domain: $DOMAIN"
            sed -i "s/flagd-admin.onova.tech/$DOMAIN/g" /etc/nginx/http.d/ssl.conf
        else
            echo "⚠️ SSL certificates not found, using default domain"
        fi
    else
        echo "ℹ️ No SSL certificates found, SSL configuration will be disabled"
    fi
}

# Function to start nginx
start_nginx() {
    echo "🌐 Setting up nginx configuration..."
    setup_ssl_config
    
    # Test nginx configuration
    nginx -t
    if [ $? -eq 0 ]; then
        echo "✅ Nginx configuration is valid"
        echo "🌐 Starting nginx on ports 8080 (HTTP) and 8443 (HTTPS)..."
    else
        echo "❌ Nginx configuration is invalid"
        exit 1
    fi
}

# Show container configuration
show_configuration() {
    echo "📋 Container Configuration:"
    echo "   • Admin Username: $FLAGD_ADMIN_USERNAME"
    echo "   • JWT Secret: ${FLAGD_JWT_SECRET:0:10}..."
    echo "   • Access Token Expiration: ${FLAGD_ACCESS_TOKEN_EXPIRATION}ms"
    echo "   • Refresh Token Expiration: ${FLAGD_REFRESH_TOKEN_EXPIRATION}ms"
    echo "   • API Port: 9090 (internal)"
    echo "   • Nginx Port: 8080 (external)"
    echo ""
}

# Main execution
main() {
    # Check if this is called for supervisord API setup
    if [ "$1" = "java" ]; then
        echo "🔧 Setting up API environment..."
        generate_jwt_secret
        set_admin_username
        generate_password_hash
        show_configuration
        echo "🏃 Starting API service..."
        exec "$@"
    else
        echo "🎯 Setting up unified container environment..."
        start_nginx
        show_configuration
        echo "🎬 Starting supervisord..."
        exec "$@"
    fi
}

# Execute main function with all arguments
main "$@"