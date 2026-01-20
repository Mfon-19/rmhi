import os
import boto3
from .settings import settings


def sqs_client():
    endpoint = os.getenv("AWS_ENDPOINT_URL", settings.AWS_ENDPOINT_URL)
    region = os.getenv("AWS_REGION", settings.AWS_REGION)
    return boto3.client(
        "sqs",
        region_name=region,
        endpoint_url=endpoint,
        aws_access_key_id=os.getenv(
            "AWS_ACCESS_KEY_ID", os.getenv("AWS_ACCESS_KEY", "test")
        ),
        aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY", "test"),
    )

