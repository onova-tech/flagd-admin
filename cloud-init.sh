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
      - "80:8080"
      - "443:8443"
    environment:
      - FLAGD_JWT_SECRET=$JWT_SECRET
      - FLAGD_ADMIN_USERNAME=$ADMIN_USERNAME
      - FLAGD_ADMIN_PASSWORD_HASH=$ADMIN_PASSWORD_BCRYPT2
      - FLAGD_AUTH_PROVIDER=jwt
      - SERVER_NAME=$SERVER_NAME
    volumes:
      - app_data:/app
      - ./certbot/conf:/etc/letsencrypt:ro

  certbot:
    image: certbot/certbot:latest
    container_name: flagd-certbot
    volumes:
      - ./certbot/conf:/etc/letsencrypt
      - /var/run/docker.sock:/var/run/docker.sock
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do \\
      docker compose -f /home/ubuntu/flagd-admin/docker-compose.yml stop app 2>/dev/null || true; \\
      sleep 5; \\
      certbot renew --standalone -d $SERVER_NAME; \\
      sleep 5; \\
      docker compose -f /home/ubuntu/flagd-admin/docker-compose.yml start app 2>/dev/null || true; \\
      sleep 12h & wait \$${!}; done;'"
    network_mode: host

volumes:
  app_data:
EOF

# Note: Nginx configuration removed - using direct deployment with app container

# Note: ACME server removed - using certbot standalone mode instead

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

# Check port 80
if lsof -i :80 >/dev/null 2>&1 || ss -tlnp | grep -q ":80 "; then
    echo "⚠️  Port 80 is already in use. Checking what service is running..."
    
    # Check for systemd services
    if systemctl is-active --quiet nginx 2>/dev/null; then
        echo "🔄 Stopping nginx service to free port 80..."
        systemctl stop nginx
        systemctl disable nginx
        echo "✅ Nginx service stopped"
    elif systemctl is-active --quiet apache2 2>/dev/null; then
        echo "🔄 Stopping apache2 service to free port 80..."
        systemctl stop apache2
        systemctl disable apache2
        echo "✅ Apache2 service stopped"
    else
        echo "🔍 Checking for Docker containers using port 80..."
        DOCKER_CONTAINERS=$(docker ps --filter "publish=80" --format "{{.ID}} {{.Names}}" 2>/dev/null)
        if [ ! -z "$DOCKER_CONTAINERS" ]; then
            echo "🔄 Stopping Docker containers using port 80..."
            echo "$DOCKER_CONTAINERS" | while read CONTAINER_ID CONTAINER_NAME; do
                echo "Stopping container: $CONTAINER_NAME ($CONTAINER_ID)"
                docker stop "$CONTAINER_ID" 2>/dev/null || true
            done
            echo "✅ Docker containers stopped"
        else
            echo "❌ Another service is using port 80. Attempting to identify..."
            echo "Running processes:"
            ps aux | grep -E "(nginx|apache|httpd)" | grep -v grep || true
            echo ""
            echo "Please identify and stop the service manually:"
            echo "  sudo ss -tulpn | grep :80"
            echo "  sudo lsof -i :80"
            echo ""
            echo "Or you can run this command to stop all services on port 80:"
            echo "  sudo fuser -k 80/tcp"
            exit 1
        fi
    fi
fi

# Check port 443
if lsof -i :443 >/dev/null 2>&1 || ss -tlnp | grep -q ":443 "; then
    echo "⚠️  Port 443 is already in use. Checking what service is running..."
    
    DOCKER_CONTAINERS=$(docker ps --filter "publish=443" --format "{{.ID}} {{.Names}}" 2>/dev/null)
    if [ ! -z "$DOCKER_CONTAINERS" ]; then
        echo "🔄 Stopping Docker containers using port 443..."
        echo "$DOCKER_CONTAINERS" | while read CONTAINER_ID CONTAINER_NAME; do
            echo "Stopping container: $CONTAINER_NAME ($CONTAINER_ID)"
            docker stop "$CONTAINER_ID" 2>/dev/null || true
        done
        echo "✅ Docker containers stopped"
    else
        echo "❌ Another service is using port 443. Please check and stop it manually:"
        echo "  sudo ss -tulpn | grep :443"
        echo "  sudo lsof -i :443"
        exit 1
    fi
fi

# Step 4: Final port check and cleanup
echo "Step 4: Final port check before starting services..."
sleep 2

# Force cleanup if ports are still occupied
if lsof -i :80 >/dev/null 2>&1; then
    echo "⚠️  Port 80 still in use, forcing cleanup..."
    fuser -k 80/tcp 2>/dev/null || true
    sleep 2
fi

if lsof -i :443 >/dev/null 2>&1; then
    echo "⚠️  Port 443 still in use, forcing cleanup..."
    fuser -k 443/tcp 2>/dev/null || true
    sleep 2
fi

echo "Step 5: Starting services..."
docker compose up -d app

# Step 6: Wait for services to be ready
echo "Step 6: Waiting for services to be ready..."
sleep 45

# Step 7: Test HTTP access
echo "Step 7: Testing HTTP access..."
if curl -f http://localhost:80 > /dev/null 2>&1; then
    echo "✅ HTTP service is working!"
else
    echo "❌ HTTP service is not working!"
    echo "Debugging information:"
    echo "1. Container status:"
    docker compose ps
    echo ""
    echo "2. Container logs:"
    docker compose logs app
    echo ""
    echo "3. Port status:"
    ss -tulpn | grep -E ":(80|8080)" || echo "No processes found on ports 80/8080"
    echo ""
    echo "4. Run debug script for more info:"
    echo "   ./debug-ports.sh"
    exit 1
fi

echo "=== Service Setup Complete ==="
echo "Application is running on: http://$SERVER_NAME"
echo ""
echo "Next steps:"
echo "1. Configure Oracle Cloud security rules (see setup-complete.log)"
echo "2. Ensure DNS A record points to this VM: $(curl -s http://169.254.169.254/opc/v2/instance/metadata | jq -r '.publicIp' 2>/dev/null || echo 'Unable to get IP')"
echo "3. Wait for DNS propagation, then run: ./setup-ssl.sh to configure HTTPS"
echo "4. Visit: http://$SERVER_NAME"
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

# Step 2: Stop app container temporarily for certbot
echo "Step 2: Preparing for SSL certificate..."
docker compose stop app

# Step 3: Ensure port 80 is free
echo "Step 3: Ensuring port 80 is free for certbot..."
sleep 3
if lsof -i :80 >/dev/null 2>&1; then
    echo "Port 80 is still in use, forcing cleanup..."
    fuser -k 80/tcp 2>/dev/null || true
    sleep 2
fi

# Step 4: Obtain SSL certificate using standalone mode
echo "Step 4: Obtaining SSL certificate using standalone mode..."
docker run --rm \
  -v /home/ubuntu/flagd-admin/certbot/conf:/etc/letsencrypt \
  -p 80:80 \
  certbot/certbot certonly --standalone \
  --email $CERTBOT_NOTIFICATION_EMAIL --agree-tos --no-eff-email -d $SERVER_NAME

# Step 5: Restart app container with SSL
echo "Step 5: Restarting app container with SSL..."
docker compose up -d app

# Step 6: Verify HTTPS setup
echo "Step 6: Verifying HTTPS setup..."
sleep 45

# Check if SSL certificates are accessible inside container
echo "Checking SSL certificates..."
if docker compose exec app test -f "/etc/letsencrypt/live/$SERVER_NAME/fullchain.pem" 2>/dev/null; then
    echo "✅ SSL certificates found in container"
else
    echo "❌ SSL certificates not found in container"
    echo "Available certificates:"
    docker compose exec app ls -la /etc/letsencrypt/live/ 2>/dev/null || echo "No certificates directory found"
    exit 1
fi

# Test HTTPS
if curl -k -f https://localhost:443 > /dev/null 2>&1; then
    echo "✅ HTTPS service is working!"
elif curl -k -f https://localhost:8443 > /dev/null 2>&1; then
    echo "✅ HTTPS service is working on container port 8443!"
else
    echo "❌ HTTPS service is not working!"
    echo "Debugging information:"
    echo "1. Container status:"
    docker compose ps
    echo ""
    echo "2. SSL certificates in container:"
    docker compose exec app find /etc/letsencrypt -name "*.pem" 2>/dev/null || echo "No certificates found"
    echo ""
    echo "3. Nginx status and configuration:"
    docker compose exec app nginx -t 2>/dev/null || echo "Nginx config test failed"
    echo ""
    echo "4. Container logs (last 50 lines):"
    docker compose logs --tail=50 app
    echo ""
    echo "5. Port check inside container:"
    docker compose exec app netstat -tlnp 2>/dev/null | grep -E ":(8080|8443)" || echo "Ports not found"
    exit 1
fi

echo "=== SSL Setup Complete ==="
echo "🎉 Your Flagd Admin is now available at: https://$SERVER_NAME"
echo "📋 Certificate auto-renewal is configured"
EOF

# Set proper permissions
echo "Setting permissions..."
chmod 644 /home/ubuntu/flagd-admin/docker-compose.yml
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

# Create SSL test script
echo "Creating ssl-test.sh..."
cat > /home/ubuntu/flagd-admin/ssl-test.sh << 'EOF'
#!/bin/bash
set -e

echo "=== SSL Certificate Test ==="
echo "This script tests the standalone mode before running setup-ssl.sh"
echo "Started at: $(date)"

cd /home/ubuntu/flagd-admin

# Step 1: Stop app container
echo "Step 1: Stopping app container..."
docker compose stop app

# Step 2: Ensure port 80 is free
echo "Step 2: Ensuring port 80 is free..."
sleep 3
if lsof -i :80 >/dev/null 2>&1; then
    echo "Port 80 is still in use, forcing cleanup..."
    fuser -k 80/tcp 2>/dev/null || true
    sleep 2
fi

# Step 3: Test certbot dry run
echo "Step 3: Testing certbot with dry run..."
docker run --rm \
  -v /home/ubuntu/flagd-admin/certbot/conf:/etc/letsencrypt \
  -p 80:80 \
  certbot/certbot certonly --standalone --dry-run \
  --email test@example.com --agree-tos --no-eff-email -d $SERVER_NAME

# Step 4: Restart app container
echo "Step 4: Restarting app container..."
docker compose start app

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
