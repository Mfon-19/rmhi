#!/bin/bash
set -euo pipefail

# --- Configuration ---
# Set the region to deploy to
AWS_REGION="${AWS_REGION:-us-east-1}"
# Set the project name (should match terraform variables)
PROJECT_NAME="${PROJECT_NAME:-eureka-scraper-v2}"
# Set the email for alerts
ALERT_EMAIL="${ALERT_EMAIL:-mfonezekel@gmail.com}"
# Set the Google API Key (ensure this is kept secure)
GOOGLE_API_KEY="${GOOGLE_API_KEY:-}"
if [ -z "$GOOGLE_API_KEY" ]; then
    echo "Error: GOOGLE_API_KEY is not set. Run: GOOGLE_API_KEY=YOUR_KEY ./deploy.sh"
    exit 1
fi

for cmd in terraform aws docker jq; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "Error: $cmd is not installed. Please install it and try again."
        exit 1
    fi
done
echo "✅ All required tools are installed."

# --- Deployment Steps ---

# 1. Apply Terraform Infrastructure
echo " Mfonudoh Applying Terraform infrastructure..."
cd infra/terraform
terraform init -upgrade
terraform apply -auto-approve \
    -var "region=$AWS_REGION" \
    -var "project=$PROJECT_NAME" \
    -var "alert_email=$ALERT_EMAIL" \
    -var "google_api_key=$GOOGLE_API_KEY"

# 2. Capture Terraform Outputs
echo " Mfonudoh Capturing Terraform outputs..."
ECR_URL=$(terraform output -raw ecr_repo_url)
LAMBDA_NAME=$(terraform output -raw lambda_name)
CLUSTER_ARN=$(terraform output -raw ecs_cluster_arn)
MIGRATION_TASK_DEF_ARN=$(terraform output -raw migration_task_def_arn)
ECS_TASKS_SG_ID=$(terraform output -raw ecs_tasks_security_group_id)
PUBLIC_SUBNETS_JSON=$(terraform output -json public_subnets)

# Check if outputs are empty
if [ -z "$ECR_URL" ] || [ -z "$LAMBDA_NAME" ] || [ -z "$CLUSTER_ARN" ] || [ -z "$MIGRATION_TASK_DEF_ARN" ] || [ -z "$ECS_TASKS_SG_ID" ]; then
    echo "❌ Error: One or more Terraform outputs are empty. Aborting."
    exit 1
fi
echo "✅ Terraform outputs captured successfully."

# 3. Build and Push Docker Image
echo " Mfonudoh Building and pushing Docker image to ECR..."
REGISTRY_URL=$(echo "$ECR_URL" | cut -d/ -f1)
cd ../.. # Go back to the project root

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY_URL"
docker build -f scraper/Dockerfile -t "$ECR_URL:latest" .
docker push "$ECR_URL:latest"
echo "✅ Docker image pushed successfully."

# 4. Apply Database Schema (inside ECS)
echo " Mfonudoh Applying database schema inside ECS..."
SUBNETS_CSV=$(echo "$PUBLIC_SUBNETS_JSON" | jq -r '. | join(",")')
NETWORK_CONFIG="awsvpcConfiguration={subnets=[$SUBNETS_CSV],securityGroups=[$ECS_TASKS_SG_ID],assignPublicIp=ENABLED}"

MIGRATION_TASK_ARN=$(aws ecs run-task \
    --cluster "$CLUSTER_ARN" \
    --launch-type FARGATE \
    --task-definition "$MIGRATION_TASK_DEF_ARN" \
    --network-configuration "$NETWORK_CONFIG" \
    --query 'tasks[0].taskArn' \
    --output text \
    --region "$AWS_REGION")

if [ -z "$MIGRATION_TASK_ARN" ] || [ "$MIGRATION_TASK_ARN" = "None" ]; then
    echo "❌ Error: Failed to start migration task."
    exit 1
fi

aws ecs wait tasks-stopped \
    --cluster "$CLUSTER_ARN" \
    --tasks "$MIGRATION_TASK_ARN" \
    --region "$AWS_REGION"

MIGRATION_EXIT_CODE=$(aws ecs describe-tasks \
    --cluster "$CLUSTER_ARN" \
    --tasks "$MIGRATION_TASK_ARN" \
    --query 'tasks[0].containers[0].exitCode' \
    --output text \
    --region "$AWS_REGION")

if [ "$MIGRATION_EXIT_CODE" != "0" ]; then
    echo "❌ Migration task failed (exit code: $MIGRATION_EXIT_CODE)."
    aws ecs describe-tasks \
        --cluster "$CLUSTER_ARN" \
        --tasks "$MIGRATION_TASK_ARN" \
        --query 'tasks[0].stoppedReason' \
        --output text \
        --region "$AWS_REGION"
    exit 1
fi

echo "✅ Database schema applied successfully."
echo " Mfonudoh IMPORTANT: Please check your email ($ALERT_EMAIL) to confirm the SNS subscription for alerts."


# 5. Run One-Time Backfill
echo " Mfonudoh Triggering the one-time backfill task..."
aws lambda invoke \
    --function-name "$LAMBDA_NAME" \
    --payload '{"mode":"backfill"}' \
    --cli-binary-format raw-in-base64-out \
    --region "$AWS_REGION" \
    backfill_output.json > /dev/null

echo "✅ Backfill task triggered. Check the ECS console and CloudWatch logs for progress."
echo " Mfonudoh Deployment complete!"
