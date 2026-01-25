#!/bin/bash

echo "🔐 Generating secure JWT secret..."
SECRET=$(openssl rand -base64 32)
echo "✅ Generated JWT secret: $SECRET"
echo ""
echo "💡 Usage:"
echo "   export FLAGD_JWT_SECRET=\"$SECRET\""
echo ""
echo "⚠️  Store this secret securely and include it in your environment variables for production deployments."