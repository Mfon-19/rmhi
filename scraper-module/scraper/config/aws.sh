export AWS_REGION=us-west-2
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_ENDPOINT_URL=http://localhost:4566

aws --endpoint-url "$AWS_ENDPOINT_URL" sqs create-queue \
  --queue-name migration-queue.fifo \
  --attributes FifoQueue=true,ContentBasedDeduplication=true

aws --endpoint-url "$AWS_ENDPOINT_URL" sqs get-queue-url --queue-name migration-queue.fifo