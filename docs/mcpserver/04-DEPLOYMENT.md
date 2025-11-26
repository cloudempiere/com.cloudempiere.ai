# Deployment Guide for iDempiere AI MCP Server

## Deployment Architectures

### Architecture 1: Embedded HTTP API + Standalone MCP Server (Recommended)

```
┌─────────────────────────────────────────────┐
│         iDempiere Instance (Port 8080)       │
│                                              │
│  ┌──────────────────────────────────────┐   │
│  │   AI Plugin (OSGi Bundle)            │   │
│  │   - AIConversationService            │   │
│  │   - AI Providers                     │   │
│  │   - Database Security Layer          │   │
│  └────────────────┬─────────────────────┘   │
│                   │                          │
│  ┌────────────────▼─────────────────────┐   │
│  │   HTTP API Endpoints (New)           │   │
│  │   - /api/ai/chat                     │   │
│  │   - /api/ai/query                    │   │
│  │   - /api/ai/context                  │   │
│  │   - /api/ai/provider/info            │   │
│  │   - /api/ai/conversation/history     │   │
│  └──────────────────────────────────────┘   │
└────────┬──────────────────────────────────┬─┘
         │ HTTP Calls                       │
         │ (Port 8080/api/ai/*)            │
         │                                  │
┌────────▼──────────────────────────────────▼┐
│   Standalone MCP Server Process (Port 9000) │
│                                             │
│   ┌────────────────────────────────────┐   │
│   │  MCP Tools                         │   │
│   │  - chat_with_context               │   │
│   │  - query_database                  │   │
│   │  - extract_context                 │   │
│   │  - get_provider_info               │   │
│   │  - get_conversation_history        │   │
│   └────────┬───────────────────────────┘   │
│            │                               │
│   ┌────────▼───────────────────────────┐   │
│   │  iDempiere API Client              │   │
│   │  (HTTP to 8080)                    │   │
│   └────────────────────────────────────┘   │
│                                             │
│   ┌────────────────────────────────────┐   │
│   │  StdIO Transport Handler           │   │
│   │  (connects to MCP clients)         │   │
│   └────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
         │
         │ StdIO / SSE
         │
┌────────▼──────────────────────────────┐
│   External MCP Clients                │
│   - Claude Code Agent                 │
│   - Custom Integration Tools          │
└───────────────────────────────────────┘
```

**Advantages**:
- Clean separation of concerns
- iDempiere can be deployed independently
- MCP server scales independently
- Easy to develop and test
- Standard HTTP makes debugging easy

**Deployment**:
1. Deploy HTTP API to iDempiere (as OSGi bundle or servlet)
2. Deploy MCP server as separate process
3. Configure MCP server to point to iDempiere API

### Architecture 2: Embedded OSGi-based MCP Server (Advanced)

```
┌─────────────────────────────────────────────────────────┐
│         iDempiere Instance (Port 8080)                  │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │   AI Plugin (OSGi Bundle)                        │   │
│  │   - AIConversationService                        │   │
│  │   - AI Providers                                 │   │
│  │   - Database Security Layer                      │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │   MCP Server (OSGi Bundle)                       │   │
│  │   - Direct access to AI Plugin services          │   │
│  │   - Uses OSGi Properties for context             │   │
│  │   - Exposes HTTP + StdIO endpoints               │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
└──────────────────┬───────────────────┬──────────────────┘
                   │ HTTP              │ StdIO
                   │ (Port 8080/mcp)   │ (Port 9000)
                   │                   │
        ┌──────────▼─────────────┐     │
        │  HTTP Clients          │     │
        └────────────────────────┘     │
                                       │
                    ┌──────────────────▼────────┐
                    │  StdIO Clients            │
                    │  (Claude Code Agents)     │
                    └───────────────────────────┘
```

**Advantages**:
- No serialization overhead
- Direct access to all plugin services
- Single JVM = simpler operations
- Can access iDempiere Properties directly

**Disadvantages**:
- More complex to develop
- OSGi bundle debugging is harder
- Cannot scale MCP server independently
- Custom transport implementation needed

**Recommended for**: Advanced deployments with specific performance requirements

## Local Development Setup

### Prerequisites

- Java 11+ (for iDempiere)
- Node.js 18+ (for MCP server)
- Docker & Docker Compose (optional, recommended)
- Git

### Option 1: Docker Compose (Recommended)

#### 1. Create docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: idempiere-db
    environment:
      POSTGRES_DB: idempiere
      POSTGRES_USER: idempiere
      POSTGRES_PASSWORD: idempiere
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U idempiere"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - idempiere-net

  # iDempiere Application Server
  idempiere:
    image: idempiere:latest
    container_name: idempiere-app
    ports:
      - "8080:8080"      # Web UI
      - "7654:7654"      # Zk port
    environment:
      DB_HOST: postgres
      DB_NAME: idempiere
      DB_USER: idempiere
      DB_PASSWORD: idempiere
      DB_PORT: 5432
      # Admin credentials for first login
      ADMIN_USER: admin
      ADMIN_PASSWORD: admin
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/webui"]
      interval: 30s
      timeout: 10s
      retries: 5
    networks:
      - idempiere-net
    volumes:
      - idempiere_data:/opt/idempiere/data

  # MCP Server
  mcp-server:
    build:
      context: ./mcp-server
      dockerfile: Dockerfile
    container_name: idempiere-mcp
    ports:
      - "9000:9000"
    environment:
      IDEMPIERE_API_URL: "http://idempiere:8080/api/ai"
      IDEMPIERE_API_KEY: "${IDEMPIERE_API_KEY:-dev-key-change-in-prod}"
      LOG_LEVEL: "info"
      NODE_ENV: "development"
    depends_on:
      idempiere:
        condition: service_healthy
    networks:
      - idempiere-net
    restart: unless-stopped

volumes:
  postgres_data:
  idempiere_data:

networks:
  idempiere-net:
    driver: bridge
```

#### 2. Create .env file

```bash
# .env
IDEMPIERE_API_KEY=dev-key-change-in-production

# Optional: Override URLs for testing
IDEMPIERE_API_URL=http://localhost:8080/api/ai
MCP_SERVER_PORT=9000
```

#### 3. Start the stack

```bash
# Build and start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Clean up everything
docker-compose down -v
```

### Option 2: Local Development (Without Docker)

#### 1. Start iDempiere locally

```bash
# Install iDempiere development environment
# Follow: https://wiki.idempiere.org/en/Development_Environment_Setup

# In iDempiere directory
./start.sh  # Linux/Mac
start.bat   # Windows
```

#### 2. Deploy HTTP API

- Add `AIRestController` to iDempiere source
- Build and deploy as OSGi bundle
- Or expose via servlet in web.xml

#### 3. Start MCP server

```bash
# In mcp-server directory
npm install
npm run build
npm start
```

#### 4. Configure & Test

```bash
# Set environment
export IDEMPIERE_API_URL=http://localhost:8080/api/ai
export IDEMPIERE_API_KEY=your-api-key

# Test API connectivity
curl http://localhost:8080/api/ai/provider/info \
  -H "X-API-Key: your-api-key"

# Test MCP server (should hang, then Ctrl+C)
timeout 5s npm start || true
```

## Production Deployment

### 1. Infrastructure Setup

#### a) AWS Deployment (ECS + RDS)

```yaml
# AWS CloudFormation / Terraform
version: '3.8'

services:
  # Load Balancer (AWS ALB)
  idempiere:
    image: "123456789.dkr.ecr.us-east-1.amazonaws.com/idempiere:latest"
    container_name: idempiere-app
    environment:
      DB_HOST: "idempiere-db.xxx.rds.amazonaws.com"
      DB_NAME: "idempiere_prod"
      # Use AWS Secrets Manager for credentials
      DB_USER: !Sub "{{resolve:secretsmanager:idempiere/db/user:SecretString:username}}"
      DB_PASSWORD: !Sub "{{resolve:secretsmanager:idempiere/db/password:SecretString:password}}"
    # TLS/HTTPS configuration
    # Security group with restricted ingress

  mcp-server:
    image: "123456789.dkr.ecr.us-east-1.amazonaws.com/idempiere-mcp-server:latest"
    environment:
      IDEMPIERE_API_URL: "https://idempiere.example.com/api/ai"
      IDEMPIERE_API_KEY: !Sub "{{resolve:secretsmanager:idempiere/api-key:SecretString:key}}"
      LOG_LEVEL: "warn"
      NODE_ENV: "production"
    # Auto-scaling group
    # CloudWatch monitoring
```

#### b) On-Premises Deployment

```bash
#!/bin/bash
# deployment.sh

# 1. Create service user
useradd -m -s /bin/bash idempiere
useradd -m -s /bin/bash mcp-server

# 2. Create directories
mkdir -p /opt/idempiere
mkdir -p /opt/mcp-server
mkdir -p /var/log/idempiere
mkdir -p /var/log/mcp-server

# 3. Deploy iDempiere
cp -r idempiere-dist/* /opt/idempiere/
chown -R idempiere:idempiere /opt/idempiere
chmod +x /opt/idempiere/start.sh

# 4. Deploy MCP server
cp -r mcp-server/dist/* /opt/mcp-server/
cp mcp-server/package.json /opt/mcp-server/
cp .env.production /opt/mcp-server/.env
chown -R mcp-server:mcp-server /opt/mcp-server

# 5. Install systemd services
cp systemd/idempiere.service /etc/systemd/system/
cp systemd/mcp-server.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable idempiere mcp-server
systemctl start idempiere mcp-server
```

### 2. Security Configuration

#### a) API Key Management

```bash
# Generate secure API key
# Option 1: Random string
openssl rand -hex 32

# Option 2: In environment secrets management
# AWS: AWS Secrets Manager
# GCP: Cloud Secret Manager
# Azure: Key Vault
# On-prem: HashiCorp Vault

# Store in environment (never in git)
export IDEMPIERE_API_KEY=$(aws secretsmanager get-secret-value \
  --secret-id idempiere/api-key \
  --query SecretString \
  --output text)
```

#### b) HTTPS/TLS Configuration

```typescript
// src/server.ts
import https from "https";
import fs from "fs";

const sslConfig = process.env.NODE_ENV === "production"
  ? {
      key: fs.readFileSync("/etc/ssl/private/key.pem"),
      cert: fs.readFileSync("/etc/ssl/certs/cert.pem"),
    }
  : null;

if (sslConfig) {
  https.createServer(sslConfig, app).listen(443);
} else {
  app.listen(9000);
}
```

#### c) Network Isolation

```bash
# Firewall rules (example: ufw on Linux)
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 8080/tcp    # iDempiere (to load balancer only)
sudo ufw allow 9000/tcp    # MCP server (to MCP clients only)
sudo ufw deny incoming     # Default deny
```

### 3. Monitoring & Logging

#### a) Structured Logging (Production)

```typescript
// Use pino with file transport
import pino from "pino";

export const logger = process.env.NODE_ENV === "production"
  ? pino(
      pino.transport({
        target: "pino/file",
        options: {
          destination: "/var/log/mcp-server/app.log",
          mkdir: true,
        },
      })
    )
  : pino({
      level: "debug",
      transport: {
        target: "pino-pretty",
      },
    });
```

#### b) Systemd Service Files

```ini
# /etc/systemd/system/mcp-server.service
[Unit]
Description=iDempiere AI MCP Server
After=network.target
Wants=idempiere.service

[Service]
Type=simple
User=mcp-server
WorkingDirectory=/opt/mcp-server
ExecStart=/usr/bin/node /opt/mcp-server/dist/index.js
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=mcp-server

# Environment files
EnvironmentFile=/etc/default/mcp-server

# Resource limits
LimitNOFILE=65536
LimitNPROC=65536

[Install]
WantedBy=multi-user.target
```

#### c) Prometheus Metrics (Optional)

```typescript
import prometheus from "prom-client";

const httpRequestDuration = new prometheus.Histogram({
  name: "http_request_duration_seconds",
  help: "Duration of HTTP requests in seconds",
  labelNames: ["method", "route", "status_code"],
});

// Export metrics endpoint
app.get("/metrics", (req, res) => {
  res.set("Content-Type", prometheus.register.contentType);
  res.end(prometheus.register.metrics());
});
```

### 4. Database Backup & Recovery

```bash
#!/bin/bash
# backup.sh - Daily backup script

BACKUP_DIR="/backups/idempiere"
DB_NAME="idempiere"
DB_USER="idempiere"
DATE=$(date +%Y%m%d_%H%M%S)

# PostgreSQL backup
pg_dump -U $DB_USER -h localhost $DB_NAME | \
  gzip > "$BACKUP_DIR/idempiere_$DATE.sql.gz"

# Cleanup old backups (keep 30 days)
find $BACKUP_DIR -name "idempiere_*.sql.gz" -mtime +30 -delete

# Upload to S3 (optional)
aws s3 cp "$BACKUP_DIR/idempiere_$DATE.sql.gz" \
  s3://my-backup-bucket/idempiere/

# Alert on failure
if [ $? -ne 0 ]; then
  # Send alert (email, Slack, etc.)
  curl -X POST https://hooks.slack.com/... \
    -d '{"text": "iDempiere backup failed"}'
fi
```

### 5. High Availability Setup

#### Multi-Instance Configuration

```yaml
# docker-compose.yml with replication

services:
  idempiere-primary:
    image: idempiere:latest
    environment:
      DB_HOST: postgres-primary
      ROLE: primary
    depends_on:
      - postgres-primary

  idempiere-replica:
    image: idempiere:latest
    environment:
      DB_HOST: postgres-replica
      ROLE: replica
    depends_on:
      - postgres-replica

  postgres-primary:
    image: postgres:15-alpine
    environment:
      POSTGRES_REPLICATION_MODE: master
      POSTGRES_REPLICATION_USER: replicator
      POSTGRES_REPLICATION_PASSWORD: secret

  postgres-replica:
    image: postgres:15-alpine
    environment:
      POSTGRES_REPLICATION_MODE: slave
      POSTGRES_MASTER_SERVICE: postgres-primary

  # Load balancer
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - idempiere-primary
      - idempiere-replica
```

## Monitoring Checklist

### Daily Checks

- [ ] Application is running (`docker-compose ps`)
- [ ] Check error logs for exceptions
- [ ] Verify backup completed successfully
- [ ] Monitor database size and free space
- [ ] Check API response times

### Weekly Checks

- [ ] Review security logs and audit trails
- [ ] Test database restore from backup
- [ ] Update dependencies if security patches available
- [ ] Review and optimize slow queries

### Monthly Checks

- [ ] Full system backup test (restore to staging)
- [ ] Capacity planning (disk, memory, CPU)
- [ ] Review and update documentation
- [ ] Security audit (access controls, API keys rotation)

### Troubleshooting Guide

#### MCP Server won't start

```bash
# Check environment variables
echo $IDEMPIERE_API_URL
echo $IDEMPIERE_API_KEY

# Test API connectivity
curl -v $IDEMPIERE_API_URL/provider/info \
  -H "X-API-Key: $IDEMPIERE_API_KEY"

# Check logs
docker logs idempiere-mcp

# Increase log level for debugging
export LOG_LEVEL=debug
npm start
```

#### High latency / slow responses

```bash
# Check API response time
time curl $IDEMPIERE_API_URL/provider/info

# Check database query performance
# In iDempiere, run slow query analysis

# Check network latency
ping idempiere

# Increase request timeout
export IDEMPIERE_REQUEST_TIMEOUT=60000
```

#### Database connection issues

```bash
# Test connection
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "SELECT 1;"

# Check connection pool
# In iDempiere logs, look for connection pool stats

# Increase pool size
export DB_POOL_SIZE=20
```

See `03-BEST_PRACTICES.md` for operational best practices.
