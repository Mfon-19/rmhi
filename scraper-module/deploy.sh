#!/bin/bash
set -euo pipefail

# --- Configuration ---
# Set your AWS CLI profile, or leave empty to use the default
AWS_PROFILE="your-profile"
# Set the region to deploy to
AWS_REGION="us-east-1"
# Set the project name (should match terraform variables)
PROJECT_NAME="eureka-scraper"
# Set the email for alerts
ALERT_EMAIL="mfonezekel@gmail.com"

# --- Pre-flight checks ---
for cmd in terraform aws docker jq psql curl; do
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
    -var "alert_email=$ALERT_EMAIL"

# 2. Capture Terraform Outputs
echo " Mfonudoh Capturing Terraform outputs..."
ECR_URL=$(terraform output -raw ecr_repo_url)
SECRET_ARN=$(terraform output -raw db_secret_arn)
DB_ENDPOINT=$(terraform output -raw db_endpoint)
LAMBDA_NAME=$(terraform output -raw lambda_name)
RDS_SG_ID=$(terraform output -raw db_security_group_id)


# Check if outputs are empty
if [ -z "$ECR_URL" ] || [ -z "$SECRET_ARN" ] || [ -z "$DB_ENDPOINT" ] || [ -z "$LAMBDA_NAME" ] || [ -z "$RDS_SG_ID" ]; then
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

# 4. Update DB Secret with full DSN
echo " Mfonudoh Checking and updating database secret..."
CURRENT_SECRET_VALUE=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" --query SecretString --output text --region "$AWS_REGION")

# Check if the secret is already a DSN. If so, get password from previous version.
if [[ "$CURRENT_SECRET_VALUE" == postgresql://* ]]; then
    echo "✅ Secret already contains a DSN. Retrieving password from previous version for psql."
    DB_DSN="$CURRENT_SECRET_VALUE"
    
    # Get the previous version of the secret
    PREVIOUS_SECRET_JSON=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" --version-stage AWSPREVIOUS --query SecretString --output text --region "$AWS_REGION")
    DB_PASS=$(echo "$PREVIOUS_SECRET_JSON" | jq -r .password)
    
    if [ -z "$DB_PASS" ]; then
        echo "❌ Could not get password from previous secret version. Manual intervention may be needed."
        exit 1
    fi
else
    echo " Mfonudoh Secret is in JSON format. Updating to full DSN..."
    DB_USER=$(echo "$CURRENT_SECRET_VALUE" | jq -r .username)
    DB_PASS=$(echo "$CURRENT_SECRET_VALUE" | jq -r .password)
    DB_DSN="postgresql://${DB_USER}:${DB_PASS}@${DB_ENDPOINT}:5432/eureka"

    aws secretsmanager put-secret-value --secret-id "$SECRET_ARN" --secret-string "$DB_DSN" --region "$AWS_REGION"
    echo "✅ Secret updated successfully."
fi

# 5. Apply Database Schema (with temporary firewall rule)
echo " Mfonudoh Applying database schema..."

# --- Temporarily open firewall for DB access ---
MY_IP=$(curl -s http://checkip.amazonaws.com)
echo " Mfonudoh Temporarily allowing access from your IP: $MY_IP"
aws ec2 authorize-security-group-ingress --group-id "$RDS_SG_ID" --protocol tcp --port 5432 --cidr "$MY_IP/32" --region "$AWS_REGION"

# Setup a trap to automatically remove the firewall rule on exit
function cleanup {
  echo " Mfonudoh Cleaning up: Removing temporary firewall rule..."
  aws ec2 revoke-security-group-ingress --group-id "$RDS_SG_ID" --protocol tcp --port 5432 --cidr "$MY_IP/32" --region "$AWS_REGION"
}
trap cleanup EXIT

# The password may contain special characters, so we pass it via an environment variable
PGPASSWORD="$DB_PASS" psql -h "$DB_ENDPOINT" -U "$DB_USER" -d "eureka" -f "db/schema.sql"
echo "✅ Database schema applied successfully."
echo " Mfonudoh IMPORTANT: Please check your email ($ALERT_EMAIL) to confirm the SNS subscription for alerts."


# 6. Run One-Time Backfill
echo " Mfonudoh Triggering the one-time backfill task..."
aws lambda invoke \
    --function-name "$LAMBDA_NAME" \
    --payload '{"mode":"backfill"}' \
    --cli-binary-format raw-in-base64-out \
    --region "$AWS_REGION" \
    backfill_output.json > /dev/null

echo "✅ Backfill task triggered. Check the ECS console and CloudWatch logs for progress."
echo " Mfonudoh Deployment complete!"
