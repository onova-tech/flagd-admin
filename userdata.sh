#!/bin/bash
# Ubuntu 24.04 Minimal - Flagd Admin Deployment Setup
# Oracle Cloud Free Tier VM

# Required environment variables:
#
export SERVER_NAME="xxx.yyy.com"  # Server host (without the protocol)
export JWT_SECRET="your-jwt-secret"  # JWT Secret String 
export ADMIN_USERNAME="admin"  # Name of the admin user of the service
export ADMIN_PASSWORD_BCRYPT2="bcrypt2-hash"  # Admin password hashed with BCRYPT2 (OBS CAST ANY $ WITH \$\$ notation)
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
mkdir -p /home/ubuntu/flagd-admin/{docker,backups,logs}

# Create .gitkeep files
touch /home/ubuntu/flagd-admin/.gitkeep
touch /home/ubuntu/flagd-admin/docker/.gitkeep
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
      FLAGD_JWT_SECRET: $JWT_SECRET
      FLAGD_ADMIN_USERNAME: $ADMIN_USERNAME
      FLAGD_ADMIN_PASSWORD_HASH: $ADMIN_PASSWORD_BCRYPT2
      FLAGD_AUTH_PROVIDER: jwt
      SERVER_NAME: $SERVER_NAME
    volumes:
      - app_data:/app
    network_mode: bridge

volumes:
  app_data:
EOF

# Create basic nginx configuration for certbot to modify
echo "Creating nginx configuration..."
cat > /etc/nginx/sites-available/flagd-admin << EOF
server {
    listen 80;
    server_name $SERVER_NAME;
    
    # Proxy to the Flagd Admin container
    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
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

iptables -I INPUT -p tcp --dport 80 -j ACCEPT
iptables -I INPUT -p tcp --dport 443 -j ACCEPT
iptables-save

# Enable nginx site
echo "Enabling nginx site..."
ln -sf /etc/nginx/sites-available/flagd-admin /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default

# Test and start nginx
echo "Testing and starting nginx..."
rm /etc/nginx/sites-enabled/default.backup
nginx -t
systemctl enable nginx
systemctl restart nginx

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

# Set proper permissions
echo "Setting permissions..."
chmod 644 /home/ubuntu/flagd-admin/docker-compose.yml
chmod 755 /home/ubuntu/flagd-admin/update.sh
chmod 755 /home/ubuntu/flagd-admin/backup.sh

# Enable Docker service
echo "Enabling and starting Docker..."
systemctl enable docker
systemctl start docker

# Add ubuntu user to docker group
usermod -aG docker ubuntu

# Download and build the application
echo "Downloading and building Flagd Admin..."
cd /home/ubuntu/flagd-admin

# Clone repository
git clone https://github.com/onova-tech/flagd-admin.git temp-repo
cp -r temp-repo/* . 2>/dev/null || true
cp -r temp-repo/.* . 2>/dev/null || true
rm -rf temp-repo .git

# Build Docker image
docker build -t flagd-admin:latest .

# Start the application
echo "Starting Flagd Admin application..."
docker compose up -d app

# Wait for application to be ready
echo "Waiting for application to start..."
sleep 30

# Setup SSL certificate with certbot --nginx
echo "Setting up SSL certificate..."
echo "Note: This will attempt to obtain a certificate for $SERVER_NAME"
echo "Make sure DNS A record points to this VM's public IP first!"

# Get public IP for reference
PUBLIC_IP=$(curl -s http://169.254.169.254/opc/v2/instance/metadata | jq -r '.publicIp' 2>/dev/null || curl -s ifconfig.me)

echo "VM Public IP: $PUBLIC_IP"
echo "Domain: $SERVER_NAME"
echo ""

# Try to obtain SSL certificate with certbot --nginx
if certbot --nginx -d "$SERVER_NAME" --email "$CERTBOT_NOTIFICATION_EMAIL" --agree-tos --no-eff-email --non-interactive; then
    echo "✅ SSL certificate obtained successfully!"
    echo "🎉 Your Flagd Admin is now available at: https://$SERVER_NAME"
    
    # Setup auto-renewal
    echo "Setting up SSL certificate auto-renewal..."
    (crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && /usr/bin/systemctl reload nginx") | crontab -
    echo "✅ SSL certificate auto-renewal configured"
else
    echo "⚠️  SSL certificate setup failed"
    echo "This is usually due to:"
    echo "1. DNS A record not pointing to this VM ($PUBLIC_IP)"
    echo "2. Oracle Cloud security rules not allowing port 80/443"
    echo "3. Domain not yet propagated"
    echo ""
    echo "Manual setup required after fixing the above issues:"
    echo "1. Configure DNS A record: $SERVER_NAME → $PUBLIC_IP"
    echo "2. Add Oracle Cloud ingress rules for ports 80 and 443"
    echo "3. Run: certbot --nginx -d $SERVER_NAME --email $CERTBOT_NOTIFICATION_EMAIL --agree-tos --no-eff-email"
    echo ""
    echo "Application is available via HTTP: http://$SERVER_NAME"
fi

# Set proper ownership
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
echo "Application Status:" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "Public IP: $PUBLIC_IP" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "Domain: $SERVER_NAME" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "If SSL failed, manual setup required:" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "1. Configure DNS A record: $SERVER_NAME → $PUBLIC_IP" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "2. Wait for DNS propagation" >> /home/ubuntu/flagd-admin/setup-complete.log
echo "3. Run: certbot --nginx -d $SERVER_NAME --email $CERTBOT_NOTIFICATION_EMAIL --agree-tos --no-eff-email" >> /home/ubuntu/flagd-admin/setup-complete.log

echo "=== Cloud-Init Setup Complete ==="
echo "VM is ready for service deployment!"
echo ""
echo "Application Status:"
echo "Public IP: $PUBLIC_IP"
echo "Domain: $SERVER_NAME"
echo ""
if certbot certificates 2>/dev/null | grep -q "$SERVER_NAME"; then
    echo "🎉 HTTPS available: https://$SERVER_NAME"
else
    echo "⚠️  HTTP only: http://$SERVER_NAME"
    echo "SSL setup failed - see setup-complete.log for manual instructions"
fi
echo ""
echo "Useful commands:"
echo "- Check status: cd ~/flagd-admin && docker compose ps"
echo "- View logs: cd ~/flagd-admin && docker compose logs app"
echo "- Update: cd ~/flagd-admin && ./update.sh"
echo "- Backup: cd ~/flagd-admin && ./backup.sh"

# Reboot the system
echo "Rebooting system..."
reboot
