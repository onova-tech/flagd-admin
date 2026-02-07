#!/bin/bash
# Ubuntu 24.04 Minimal - Flagd Admin Deployment Setup
# Oracle Cloud Free Tier VM

# Required environment variables:
#
export SERVER_NAME="xxx.yyy.com"  # Server host (without the protocol)
export JWT_SECRET="your-jwt-secret"  # JWT Secret String
export ADMIN_USERNAME="admin"  # Name of the admin user of the service
export ADMIN_PASSWORD_BCRYPT2="bcrypt2-hash"  # Admin password hashed with BCRYPT2
export CERTBOT_NOTIFICATION_EMAIL="email@example.com"  # Notification e-mail for certbot
#
# Set these variables before running the script or export them in your shell
#

set -e

# Check for required environment variables
echo "Checking required environment variables..."
if [ -z "$SERVER_NAME" ]; then
    echo "Error: SERVER_NAME environment variable is required"
    exit 1
fi
if [ -z "$JWT_SECRET" ]; then
    echo "Error: JWT_SECRET environment variable is required"
    exit 1
fi
if [ -z "$ADMIN_USERNAME" ]; then
    echo "Error: ADMIN_USERNAME environment variable is required"
    exit 1
fi
if [ -z "$ADMIN_PASSWORD_BCRYPT2" ]; then
    echo "Error: ADMIN_PASSWORD_BCRYPT2 environment variable is required"
    exit 1
fi
if [ -z "$CERTBOT_NOTIFICATION_EMAIL" ]; then
    echo "Error: CERTBOT_NOTIFICATION_EMAIL environment variable is required"
    exit 1
fi

echo "=== Starting Flagd Admin Cloud-Init Setup ==="
echo "Started at: $(date)"

# Basic system configuration
echo "Configuring system settings..."
hostnamectl set-hostname flagd-admin-vm
timedatectl set-timezone UTC

# Update /etc/hosts
echo "127.0.0.1 localhost" > /etc/hosts
echo "127.0.1.1 flagd-admin-vm" >> /etc/hosts
echo "::1 ip6-localhost ip6-loopback" >> /etc/hosts
echo "fe00::0 ip6-localnet" >> /etc/hosts
echo "ff00::0 ip6-mcastprefix" >> /etc/hosts
echo "ff02::1 ip6-allnodes" >> /etc/hosts
echo "ff02::2 ip6-allrouters" >> /etc/hosts

# Package management
echo "Updating and installing packages..."
apt-get update
apt-get upgrade -y

apt-get install ca-certificates curl gnupg lsb-release

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
"deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
"$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update

# Install packages
apt-get install -y \
    wget \
    git \
    htop \
    nano \
    unzip \
    dnsutils \
    jq \
    apt-transport-https \
    docker.io \
    docker-compose-plugin \
    nginx \
    certbot \
    python3-certbot-nginx \
    python3

# Create swap file for 1GB RAM VM
echo "Creating swap file..."
fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

# Create directory structure
echo "Creating directory structure..."
mkdir -p /home/ubuntu/flagd-admin/{docker,certbot/conf,backups,logs}

# Create .gitkeep files
touch /home/ubuntu/flagd-admin/.gitkeep
touch /home/ubuntu/flagd-admin/docker/.gitkeep
touch /home/ubuntu/flagd-admin/certbot/conf/.gitkeep
# Note: certbot/www directory removed - not needed with standalone mode
touch /home/ubuntu/flagd-admin/backups/.gitkeep

# Docker Compose configuration
echo "Creating docker-compose.yml..."
cat > /home/ubuntu/flagd-admin/docker-compose.yml << EOF
version: '3.8'

services:
  app:
    image: flagd-admin:latest
    container_name: flagd-admin
    restart: unless-stopped
    ports:
      - "127.0.0.1:8081:8080"
    environment:
      - FLAGD_JWT_SECRET=$JWT_SECRET
      - FLAGD_ADMIN_USERNAME=$ADMIN_USERNAME
      - FLAGD_ADMIN_PASSWORD_HASH=$ADMIN_PASSWORD_BCRYPT2
      - FLAGD_AUTH_PROVIDER=jwt
      - SERVER_NAME=$SERVER_NAME
    volumes:
      - app_data:/app
    network_mode: bridge

volumes:
  app_data:
EOF

# Create server nginx configuration for HTTP (redirect to HTTPS)
echo "Creating nginx HTTP configuration..."
cat > /home/ubuntu/flagd-admin/nginx-http.conf << 'EOF'
server {
    listen 80;
    server_name _;
    
    # Redirect all HTTP traffic to HTTPS
    return 301 https://$host$request_uri;
}
EOF

# Create server nginx configuration for HTTPS with proxy to container
echo "Creating nginx HTTPS configuration..."
cat > /home/ubuntu/flagd-admin/nginx-https.conf << 'EOF'
server {
    listen 443 ssl http2;
    server_name _;
    
    # SSL configuration (will be updated by certbot)
    ssl_certificate /etc/letsencrypt/live/DOMAIN_PLACEHOLDER/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/DOMAIN_PLACEHOLDER/privkey.pem;
    
    # SSL settings
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-SHA256:ECDHE-RSA-AES256-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Forwarded-Proto https always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options SAMEORIGIN always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    
    # Proxy to the Flagd Admin container
    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeout settings
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
        
        # Handle large uploads
        client_max_body_size 10M;
    }
    
    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/json
        application/javascript
        application/xml+rss
        application/atom+xml
        image/svg+xml;
}
EOF

# Create nginx sites-available configuration
echo "Setting up nginx sites configuration..."
cat > /home/ubuntu/flagd-admin/nginx-setup.sh << 'EOF'
#!/bin/bash
set -e

echo "Setting up nginx configuration..."

# Backup default nginx configuration
mv /etc/nginx/sites-enabled/default /etc/nginx/sites-enabled/default.backup 2>/dev/null || true

# Copy our configurations
cp /home/ubuntu/flagd-admin/nginx-http.conf /etc/nginx/sites-available/flagd-admin-http
cp /home/ubuntu/flagd-admin/nginx-https.conf /etc/nginx/sites-available/flagd-admin-https

# Create symbolic links
ln -sf /etc/nginx/sites-available/flagd-admin-http /etc/nginx/sites-enabled/
# Note: HTTPS config will be enabled after SSL certificate is obtained

# Test nginx configuration
nginx -t

# Enable and start nginx
systemctl enable nginx
systemctl restart nginx

echo "Nginx configuration completed"
EOF

# Update script
echo "Creating update.sh..."
cat > /home/ubuntu/flagd-admin/update.sh << 'EOF'
#!/bin/bash
set -e

echo "=== Flagd Admin Update Script ==="
echo "Started at: $(date)"

cd /home/ubuntu/flagd-admin

echo "Checking for updates..."
git fetch origin
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)

if [ "$LOCAL" != "$REMOTE" ]; then
    echo "Updates found! Deploying..."
    git pull origin main
    docker compose down
    docker build -t flagd-admin:latest .
    docker compose up -d
    echo "Deployment complete!"
    docker compose ps
else
    echo "No updates available."
fi

echo "Completed at: $(date)"
EOF

# Backup script
echo "Creating backup.sh..."
cat > /home/ubuntu/flagd-admin/backup.sh << 'EOF'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
cd /home/ubuntu/flagd-admin
docker compose exec app tar -czf /tmp/flagd-backup-$DATE.tar.gz -C /app .
docker cp flagd-admin:/tmp/flagd-backup-$DATE.tar.gz ./backups/
echo "Backup completed: flagd-backup-$DATE.tar.gz"
EOF

# Service startup script (SEPARATE - to be run manually after DNS setup)
echo "Creating start-services.sh..."
cat > /home/ubuntu/flagd-admin/start-services.sh << 'EOF'
#!/bin/bash
set -e

echo "=== Flagd Admin Service Startup ==="
echo "Started at: $(date)"

cd /home/ubuntu/flagd-admin

# Step 1: Download or update repository in separate folder
echo "Step 1: Setting up repository..."
REPO_DIR="/home/ubuntu/flagd-admin-repo"

if [ ! -d "$REPO_DIR" ]; then
    echo "Cloning repository to separate folder..."
    git clone https://github.com/onova-tech/flagd-admin.git "$REPO_DIR"
else
    echo "Repository exists, pulling latest..."
    cd "$REPO_DIR"
    git pull origin main
    cd /home/ubuntu/flagd-admin
fi

# Copy source code from repository to current directory
echo "Copying source code from repository..."
cp -r "$REPO_DIR"/* . 2>/dev/null || true
cp -r "$REPO_DIR"/.* . 2>/dev/null || true

# Remove the .git directory to keep current directory independent
rm -rf .git

# Step 2: Build Docker image
echo "Step 2: Building Docker image..."
docker build -t flagd-admin:latest .

# Step 3: Check for port conflicts and start services
echo "Step 3: Checking for port conflicts..."

# Check port 80 (nginx should be running)
if systemctl is-active --quiet nginx 2>/dev/null; then
    echo "✅ Nginx is running on port 80 - that's expected!"
else
    echo "⚠️  Nginx is not running, starting it..."
    systemctl start nginx
    if systemctl is-active --quiet nginx 2>/dev/null; then
        echo "✅ Nginx started successfully"
    else
        echo "❌ Failed to start nginx"
        exit 1
    fi
fi
    fi
fi

# Check port 443 (should be free until SSL is set up)
if ss -tlnp | grep -q ":443 "; then
    echo "⚠️  Port 443 is already in use. Checking what service is running..."
    
    if systemctl is-active --quiet nginx 2>/dev/null; then
        echo "🔄 Restarting nginx to ensure clean state..."
        systemctl restart nginx
        echo "✅ Nginx restarted"
    else
        echo "❌ Another service is using port 443. Please check and stop it manually:"
        echo "  sudo ss -tulpn | grep :443"
        echo "  sudo lsof -i :443"
        exit 1
    fi
else
    echo "✅ Port 443 is free (expected before SSL setup)"
fi

# Step 4: Final port check and cleanup
echo "Step 4: Final port check before starting services..."
sleep 2

# No need for force cleanup - nginx should manage port 80
echo "✅ Port management handled by nginx"

echo "Step 5: Starting services..."
docker compose up -d app

# Step 6: Wait for services to be ready
echo "Step 6: Waiting for services to be ready..."
sleep 45

# Step 7: Test HTTP access (should redirect to HTTPS setup later)
echo "Step 7: Testing HTTP access..."
if curl -f http://localhost:80 > /dev/null 2>&1; then
    echo "✅ HTTP service is working!"
else
    echo "❌ HTTP service is not working!"
    echo "Debugging information:"
    echo "1. Nginx status:"
    systemctl status nginx --no-pager
    echo ""
    echo "2. Container status:"
    docker compose ps
    echo ""
    echo "3. Container logs:"
    docker compose logs app
    echo ""
    echo "4. Port status:"
    ss -tulpn | grep -E ":(80|8081)" || echo "No processes found on ports 80/8081"
    echo ""
    echo "5. Nginx configuration test:"
    nginx -t
    exit 1
fi

echo "=== Service Setup Complete ==="
echo "Application is running on: http://$SERVER_NAME (HTTP to HTTPS redirect)"
echo "Container is running internally on port 8081"
echo ""
echo "Next steps:"
echo "1. Configure Oracle Cloud security rules (see setup-complete.log)"
echo "2. Ensure DNS A record points to this VM: $(curl -s http://169.254.169.254/opc/v2/instance/metadata | jq -r '.publicIp' 2>/dev/null || echo 'Unable to get IP')"
echo "3. Wait for DNS propagation, then run: ./ssl-test.sh to test SSL setup"
echo "4. Run: ./setup-ssl.sh to configure HTTPS"
echo "5. Visit: https://$SERVER_NAME (after SSL setup)"
EOF

# SSL setup script (SEPARATE - to be run manually after DNS propagation)
echo "Creating setup-ssl.sh..."
cat > /home/ubuntu/flagd-admin/setup-ssl.sh << 'EOF'
#!/bin/bash
set -e

echo "=== SSL Certificate Setup ==="
echo "Started at: $(date)"

cd /home/ubuntu/flagd-admin

# Step 1: Verify DNS resolution matches this VM
echo "Step 1: Verifying DNS resolution..."
MY_IP=$(curl -s http://169.254.169.254/opc/v2/instance/metadata | jq -r '.publicIp' 2>/dev/null || curl -s ifconfig.me)
DNS_IP=$(dig +short $SERVER_NAME | head -1)

if [ -z "$DNS_IP" ]; then
    echo "❌ DNS resolution not working for $SERVER_NAME!"
    echo "Please ensure your A record points to this VM's public IP"
    exit 1
fi

if [ "$DNS_IP" != "$MY_IP" ]; then
    echo "❌ DNS resolves to $DNS_IP but this VM IP is $MY_IP"
    echo "Please update your A record to point to $MY_IP"
    exit 1
fi

echo "✅ DNS resolution working! ($SERVER_NAME → $MY_IP)"

# Step 2: Update HTTPS nginx configuration with actual domain
echo "Step 2: Updating nginx HTTPS configuration..."
sed "s/DOMAIN_PLACEHOLDER/$SERVER_NAME/g" /home/ubuntu/flagd-admin/nginx-https.conf > /tmp/nginx-https-updated.conf
mv /tmp/nginx-https-updated.conf /etc/nginx/sites-available/flagd-admin-https

# Step 3: Ensure nginx is running on port 80 for certbot challenge
echo "Step 3: Preparing nginx for certbot..."
systemctl restart nginx

# Step 4: Obtain SSL certificate using nginx plugin
echo "Step 4: Obtaining SSL certificate using certbot nginx plugin..."
certbot --nginx -d $SERVER_NAME --email $CERTBOT_NOTIFICATION_EMAIL --agree-tos --no-eff-email --non-interactive

# Step 5: Enable HTTPS site
echo "Step 5: Enabling HTTPS site..."
ln -sf /etc/nginx/sites-available/flagd-admin-https /etc/nginx/sites-enabled/

# Step 6: Test and reload nginx
echo "Step 6: Testing and reloading nginx..."
nginx -t
systemctl reload nginx

# Step 7: Verify HTTPS setup
echo "Step 7: Verifying HTTPS setup..."
sleep 10

# Test HTTPS
if curl -f https://localhost:443 > /dev/null 2>&1; then
    echo "✅ HTTPS service is working!"
else
    echo "❌ HTTPS service is not working!"
    echo "Debugging information:"
    echo "1. Nginx status:"
    systemctl status nginx --no-pager
    echo ""
    echo "2. Nginx configuration test:"
    nginx -t
    echo ""
    echo "3. SSL certificates:"
    ls -la /etc/letsencrypt/live/$SERVER_NAME/ 2>/dev/null || echo "No certificates found"
    echo ""
    echo "4. Port status:"
    ss -tulpn | grep -E ":(80|443)" || echo "No processes found on ports 80/443"
    exit 1
fi

# Step 8: Setup auto-renewal
echo "Step 8: Setting up SSL certificate auto-renewal..."
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && /usr/bin/systemctl reload nginx") | crontab -

echo "=== SSL Setup Complete ==="
echo "🎉 Your Flagd Admin is now available at: https://$SERVER_NAME"
echo "📋 Certificate auto-renewal is configured via cron"
EOF

# Set proper permissions
echo "Setting permissions..."
chmod 644 /home/ubuntu/flagd-admin/docker-compose.yml
chmod 644 /home/ubuntu/flagd-admin/nginx-http.conf
chmod 644 /home/ubuntu/flagd-admin/nginx-https.conf
chmod 755 /home/ubuntu/flagd-admin/nginx-setup.sh
chmod 755 /home/ubuntu/flagd-admin/update.sh
chmod 755 /home/ubuntu/flagd-admin/backup.sh
chmod 755 /home/ubuntu/flagd-admin/start-services.sh
chmod 755 /home/ubuntu/flagd-admin/setup-ssl.sh
chmod 755 /home/ubuntu/flagd-admin/ssl-test.sh

# Enable Docker service
echo "Enabling and starting Docker..."
systemctl enable docker
systemctl start docker

# Add ubuntu user to docker group
usermod -aG docker ubuntu

# Setup nginx
echo "Setting up nginx..."
chmod +x /home/ubuntu/flagd-admin/nginx-setup.sh
/home/ubuntu/flagd-admin/nginx-setup.sh

# Create SSL test script
echo "Creating ssl-test.sh..."
cat > /home/ubuntu/flagd-admin/ssl-test.sh << 'EOF'
#!/bin/bash
set -e

echo "=== SSL Certificate Test ==="
echo "This script tests certbot with nginx plugin before running setup-ssl.sh"
echo "Started at: $(date)"

cd /home/ubuntu/flagd-admin

# Step 1: Ensure nginx is running on port 80
echo "Step 1: Ensuring nginx is running..."
systemctl start nginx

# Step 2: Test certbot dry run with nginx plugin
echo "Step 2: Testing certbot with dry run..."
certbot --nginx -d $SERVER_NAME --email test@example.com --agree-tos --no-eff-email --dry-run --non-interactive

echo "=== SSL Test Complete ==="
echo "✅ Dry run successful! You can now run ./setup-ssl.sh"
EOF

# Set proper ownership of project directory
echo "Setting ownership..."
chown -R ubuntu:ubuntu /home/ubuntu/flagd-admin

# Display completion message
echo "=== Cloud-Init Setup Complete ===" > /home/ubuntu/flagd-admin/setup-complete.log
echo "VM is ready for service deployment!" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "IMPORTANT: Oracle Cloud Security Configuration Required!" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "1. Go to Oracle Cloud Console → Compute → Instances" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "2. Select this instance and click 'Virtual Cloud Network'" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "3. Click 'Subnet' → 'Default Security List'" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "4. Add Ingress Rules:" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "   - Source: 0.0.0.0/0, Destination Port: 80, Protocol: TCP" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "   - Source: 0.0.0.0/0, Destination Port: 443, Protocol: TCP" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "After security rules are configured:" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "5. SSH into the VM" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "6. Run: cd ~/flagd-admin && ./start-services.sh" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "7. Configure DNS A record pointing to this VM's public IP" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "8. Run: ./ssl-test.sh to test the SSL setup" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "9. Run: ./setup-ssl.sh to configure HTTPS" >> /home/ubuntu/flagd-admin/setup-complete.log

echo "=== Cloud-Init Setup Complete ==="
echo "VM is ready for service deployment!"
echo ""
echo "IMPORTANT: Oracle Cloud Security Configuration Required!"
echo "1. Go to Oracle Cloud Console → Compute → Instances"
echo "2. Select this instance and click 'Virtual Cloud Network'"
echo "3. Click 'Subnet' → 'Default Security List'"
echo "4. Add Ingress Rules:"
echo "   - Source: 0.0.0.0/0, Destination Port: 80, Protocol: TCP"
echo "   - Source: 0.0.0.0/0, Destination Port: 443, Protocol: TCP"
echo ""
echo "After security rules are configured:"
echo "5. SSH into the VM"
echo "6. Run: cd ~/flagd-admin && ./start-services.sh"
echo "7. Configure DNS A record pointing to this VM's public IP"
echo "8. Run: ./ssl-test.sh to test the SSL setup"
echo "9. Run: ./setup-ssl.sh to configure HTTPS"

# Reboot the system
echo "Rebooting system..."
reboot
