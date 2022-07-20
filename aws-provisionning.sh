TOPIC_NAME="topic_all.fifo"
QUEUE_NAME_TENANT_1_RISK_MANAGEMENT="tenant_1_risk_management.fifo"
QUEUE_NAME_TENANT_1_XVA_FEE="tenant_1_xva_fee.fifo"
QUEUE_NAME_TENANT_2_ALL="tenant_2_all.fifo"

ALL_QUEUES=($QUEUE_NAME_TENANT_1_RISK_MANAGEMENT $QUEUE_NAME_TENANT_1_XVA_FEE $QUEUE_NAME_TENANT_2_ALL)

echo "#####################################################"
echo "Will provision the required infra. "
echo "WARNING : in this sample, we will use a single topic for all tenants. This is likely not the case in prod for proper security and 'error proof' of the solution"

read  -n 1 -p $"Press key to continue" mainmenuinput

TOPIC_ARN=$(aws sns create-topic \
  --name "$TOPIC_NAME" \
  --attributes FifoTopic=true \
  --query TopicArn \
  --output text)
echo "SNS - the following topic [$TOPIC_ARN] has been created"

for QUEUE_NAME in "${ALL_QUEUES[@]}"; do
  echo "Will create queue [$QUEUE_NAME]"
  QUEUE_URL=$(aws sqs create-queue --queue-name "$QUEUE_NAME" --attributes FifoQueue=true --query QueueUrl --output text)
  QUEUE_ARN_JSON=$(aws sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names QueueArn --output json)
  QUEUE_ARN=$(echo $QUEUE_ARN_JSON | jq -r '.Attributes.QueueArn')
  echo "SQS - the following queue ARN [$QUEUE_ARN] and queue-url [$QUEUE_URL] has been created"

  echo '{"Policy": "{\"Version\": \"2012-10-17\", \"Id\": \"arn:aws:sqs:us-east-1:427396468640:undefined/SQSDefaultPolicy\",\"Statement\": [{\"Sid\": \"topic-subscription-arn:aws:sns:us-east-1:427396468640:topic_all_tenant.fifo\",\"Effect\": \"Allow\",\"Principal\": {\"AWS\": \"*\"},\"Action\": \"SQS:SendMessage\",\"Resource\": \"'"$QUEUE_ARN"'\",\"Condition\": {\"ArnLike\": {\"aws:SourceArn\": \"'"$TOPIC_ARN"'\"}}}]}"}' > ./aws-sqs-attributes.json
  TMP=$(aws sqs set-queue-attributes --queue-url "$QUEUE_URL" --attributes file://aws-sqs-attributes.json )

  #Set the subscription, incl. FilterPolicy (different for each subsription
  SUBSCRIPTION_ARN=$(aws sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$QUEUE_ARN" --attributes file://aws-subscription-attributes-"$QUEUE_NAME".json --no-return-subscription-arn --query SubscriptionArn)
  echo "Subscription [$SUBSCRIPTION_ARN] has been created with Attributes [file://aws-subscription-attributes"$QUEUE_NAME".json]"
done

echo "#####################################################"
echo "Overall summary of the provisionned infra"
read  -n 1 -p "Press key to continue" mainmenuinput

TMP=$(aws sns list-topics)
echo $TMP
read  -n 1 -p "Press key to continue" mainmenuinput

TMP=$(aws sqs list-queues)
echo $TMP
read  -n 1 -p "Press key to continue" mainmenuinput

TMP=$(aws sns list-subscriptions)
echo $TMP
read  -n 1 -p "Press key to continue" mainmenuinput