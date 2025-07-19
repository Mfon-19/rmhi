#!/bin/bash

# RMHI Scraping Service Deployment Script
# Usage: ./deploy.sh [environment] [version]

set -e

ENVIRONMENT=${1:-docker}
VERSION=${2:-latest}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "Deploying RMHI Scraping Service"
echo "Environment: $ENVIRONMENT"
echo "Version: $VERSION"
echo "Project Directory: $PROJECT_DIR"

# Function to check prerequisites
check_prerequisites() {
    echo "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo "ERROR: Docker is not installed"
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        echo "ERROR: Docker Compose is not installed"
        exit 1
    fi
    
    # Check environment file
    if [ ! -f "$PROJECT_DIR/.env" ]; then
        echo "WARNING: .env file not found. Copying from .env.example"
        cp "$PROJECT_DIR/.env.example" "$PROJECT_DIR/.env"
        echo "Please edit .env file with your configuration before proceeding"
        exit 1
    fi
    
    echo "Prerequisites check passed"
}

# Function to build images
build_images() {
    echo "Building Docker images..."
    
    cd "$PROJECT_DIR"
    
    # Build main application
    docker build -t rmhi-app:$VERSION .
    
    # Build scraping service
    docker build -f Dockerfile.scraping -t rmhi-scraper:$VERSION .
    
    echo "Docker images built successfully"
}

# Function to deploy services
deploy_services() {
    echo "Deploying services..."
    
    cd "$PROJECT_DIR"
    
    # Set environment variables
    export SPRING_PROFILES_ACTIVE=$ENVIRONMENT
    export IMAGE_VERSION=$VERSION
    
    # Stop existing services
    docker-compose down
    
    # Start services
    docker-compose up -d
    
    echo "Services deployed successfully"
}

# Function to wait for services to be ready
wait_for_services() {
    echo "Waiting for services to be ready..."
    
    # Wait for database
    echo "Waiting for database..."
    timeout=60
    while [ $timeout -gt 0 ]; do
        if docker exec rmhi-postgres pg_isready -U rmhi_user -d rmhi &> /dev/null; then
            echo "Database is ready"
            break
        fi
        sleep 2
        timeout=$((timeout - 2))
    done
    
    if [ $timeout -le 0 ]; then
        echo "ERROR: Database failed to start within timeout"
        exit 1
    fi
    
    # Wait for scraping service
    echo "Waiting for scraping service..."
    timeout=120
    while [ $timeout -gt 0 ]; do
        if curl -f http://localhost:8081/actuator/health &> /dev/null; then
            echo "Scraping service is ready"
            break
        fi
        sleep 5
        timeout=$((timeout - 5))
    done
    
    if [ $timeout -le 0 ]; then
        echo "ERROR: Scraping service failed to start within timeout"
        exit 1
    fi
    
    echo "All services are ready"
}

# Function to run post-deployment checks
post_deployment_checks() {
    echo "Running post-deployment checks..."
    
    # Check service health
    echo "Checking service health..."
    health_response=$(curl -s http://localhost:8081/actuator/health)
    if echo "$health_response" | grep -q '"status":"UP"'; then
        echo "✓ Service health check passed"
    else
        echo "✗ Service health check failed"
        echo "Response: $health_response"
        exit 1
    fi
    
    # Check database connectivity
    echo "Checking database connectivity..."
    if docker exec rmhi-postgres psql -U rmhi_user -d rmhi -c "SELECT 1;" &> /dev/null; then
        echo "✓ Database connectivity check passed"
    else
        echo "✗ Database connectivity check failed"
        exit 1
    fi
    
    # Check scraping configuration
    echo "Checking scraping configuration..."
    config_count=$(docker exec rmhi-postgres psql -U rmhi_user -d rmhi -t -c "SELECT COUNT(*) FROM scraping_sources WHERE enabled = true;")
    if [ "$config_count" -gt 0 ]; then
        echo "✓ Scraping configuration check passed ($config_count sources configured)"
    else
        echo "⚠ No scraping sources configured"
    fi
    
    echo "Post-deployment checks completed"
}

# Function to show deployment summary
show_summary() {
    echo ""
    echo "=== Deployment Summary ==="
    echo "Environment: $ENVIRONMENT"
    echo "Version: $VERSION"
    echo "Services:"
    docker-compose ps
    echo ""
    echo "Service URLs:"
    echo "  - Main Application: http://localhost:8080"
    echo "  - Scraping Service: http://localhost:8081"
    echo "  - Health Check: http://localhost:8081/actuator/health"
    echo "  - Metrics: http://localhost:8081/actuator/metrics"
    echo ""
    echo "Logs:"
    echo "  - View logs: docker-compose logs -f rmhi-scraper"
    echo "  - View all logs: docker-compose logs -f"
    echo ""
    echo "Deployment completed successfully!"
}

# Main deployment flow
main() {
    check_prerequisites
    build_images
    deploy_services
    wait_for_services
    post_deployment_checks
    show_summary
}

# Handle script arguments
case "$1" in
    "help"|"-h"|"--help")
        echo "Usage: $0 [environment] [version]"
        echo ""
        echo "Arguments:"
        echo "  environment  Target environment (docker, production) [default: docker]"
        echo "  version      Image version tag [default: latest]"
        echo ""
        echo "Examples:"
        echo "  $0                    # Deploy with defaults"
        echo "  $0 production v1.2.0  # Deploy to production with specific version"
        exit 0
        ;;
    *)
        main
        ;;
esac