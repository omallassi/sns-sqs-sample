QUEUE_URL=https://sqs.us-east-1.amazonaws.com/427396468640/tenant_2_all.fifo

echo "#####################################################"
echo "The following will wait and consume messages from queue [$QUEUE_URL]"

read  -n 1 -p "Press key to continue" mainmenuinput

java -cp ./build/libs/bpaas-sns-sqs-sample-1.0-SNAPSHOT.jar org.bpaas.sample.ConsumerMain --queue-url $QUEUE_URL

read  -n 1 -p "Press key to continue" mainmenuinput