# RMHI Scraping Service Deployment

## Quick Start

### Docker Compose (Recommended for Development)

1. **Setup Environment**

   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

2. **Deploy Services**

   ```bash
   ./scripts/deploy.sh
   ```

3. **Verify Deployment**
   ```bash
   curl http://localhost:8081/actuator/health
   ```

### Production Deployment

#### Option 1: Docker Compose

```bash
# Set production environment
export SPRING_PROFILES_ACTIVE=production

# Deploy with production configuration
./scripts/deploy.sh production v1.0.0
```

#### Option 2: Kubernetes

```bash
# Create namespace and secrets
kubectl apply -f k8s/namespace.yaml

# Create secrets (replace with actual values)
kubectl create secret generic rmhi-secrets \
  --namespace=rmhi-scraping \
  --from-literal=database-url="jdbc:postgresql://your-db:5432/rmhi" \
  --from-literal=database-username="rmhi_user" \
  --from-literal=database-password="your-password" \
  --from-literal=redis-host="your-redis-host" \
  --from-literal=redis-password="your-redis-password" \
  --from-literal=gemini-api-key="your-gemini-key"

# Deploy configuration and application
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
```

## Configuration

### Environment Variables

| Variable                   | Description           | Default       | Required |
| -------------------------- | --------------------- | ------------- | -------- |
| `POSTGRES_DB`              | Database name         | rmhi          | Yes      |
| `POSTGRES_USER`            | Database user         | rmhi_user     | Yes      |
| `POSTGRES_PASSWORD`        | Database password     | -             | Yes      |
| `GEMINI_API_KEY`           | Google Gemini API key | -             | Yes      |
| `SCRAPING_ENABLED`         | Enable scraping jobs  | true          | No       |
| `SCRAPING_CRON_EXPRESSION` | Cron schedule         | 0 0 2 \* \* ? | No       |

### Application Profiles

- **default**: Development configuration
- **docker**: Docker environment configuration
- **production**: Production environment configuration

## Monitoring

### Health Checks

- **Application Health**: `http://localhost:8081/actuator/health`
- **Readiness**: `http://localhost:8081/actuator/health/readiness`
- **Liveness**: `http://localhost:8081/actuator/health/liveness`

### Metrics

- **Prometheus Metrics**: `http://localhost:8081/actuator/prometheus`
- **Application Metrics**: `http://localhost:8081/actuator/metrics`

### Monitoring Stack

Deploy the monitoring stack:

```bash
# Start monitoring services
docker-compose -f monitoring/docker-compose.monitoring.yml up -d

# Access dashboards
# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin)
# AlertManager: http://localhost:9093
```

## Backup and Recovery

### Automated Backups

Backups are configured to run automatically:

- **Full Database Backup**: Daily at 1:00 AM
- **Incremental Backup**: Every 6 hours
- **Configuration Backup**: Daily at 11:30 PM

### Manual Backup

```bash
# Database backup
docker exec rmhi-postgres pg_dump -U rmhi_user -d rmhi > backup.sql

# Configuration backup
tar -czf config-backup.tar.gz \
  src/main/resources/application*.properties \
  docker-compose.yml \
  monitoring/
```

### Recovery

See `docs/backup-recovery-procedures.md` for detailed recovery procedures.

## Troubleshooting

### Common Issues

#### Service Won't Start

```bash
# Check logs
docker-compose logs rmhi-scraper

# Check health
curl http://localhost:8081/actuator/health
```

#### Database Connection Issues

```bash
# Test database connectivity
docker exec rmhi-postgres pg_isready -U rmhi_user -d rmhi

# Check database logs
docker-compose logs postgres
```

#### AI Service Issues

```bash
# Check API key configuration
docker exec rmhi-scraper env | grep GEMINI

# Test AI service connectivity
curl -s http://localhost:8081/actuator/metrics | grep ai_transformation
```

### Log Analysis

```bash
# View real-time logs
docker-compose logs -f rmhi-scraper

# Search for errors
docker-compose logs rmhi-scraper | grep ERROR

# Check specific time range
docker-compose logs --since="2025-01-15T10:00:00" rmhi-scraper
```

## Security

### Secrets Management

- Store sensitive data in environment variables
- Use Docker secrets or Kubernetes secrets in production
- Rotate API keys and passwords regularly

### Network Security

- Use internal networks for service communication
- Restrict external access to management endpoints
- Enable SSL/TLS for production deployments

## Performance Tuning

### JVM Settings

```bash
# Adjust memory settings
export JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

### Database Optimization

```properties
# Connection pool tuning
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### Scraping Performance

```properties
# Concurrent scraping
scraping.max-concurrent-scrapers=5

# AI transformation batch size
ai.transformation.batch-size=10
```

## Scaling

### Horizontal Scaling

```bash
# Scale scraping service
docker-compose up -d --scale rmhi-scraper=3
```

### Kubernetes Scaling

```bash
# Scale deployment
kubectl scale deployment rmhi-scraper --replicas=5 -n rmhi-scraping
```

## Support

For additional support:

- **Documentation**: See `docs/` directory
- **Operational Runbook**: `docs/operational-runbook.md`
- **Backup Procedures**: `docs/backup-recovery-procedures.md`
- **Development Team**: dev-team@company.com
- **Operations Team**: ops-team@company.com
