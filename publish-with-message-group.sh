TOPIC_ARN=arn:aws:sns:us-east-1:427396468640:topic_all.fifo

echo "#####################################################"
echo "The following will inject messages from two distinct tenants, multiple activities..."
echo "Publishing to [$TOPIC_ARN]"
read  -n 1 -p "Press key to continue" mainmenuinput

echo -e '\e[*** Publishing for tenant-id 2\e[0m'
java -cp ./build/libs/bpaas-sns-sqs-sample-1.0-SNAPSHOT.jar org.bpaas.sample.PublisherMain --topic-arn $TOPIC_ARN --tenant-id 2 --message-num 60 --message-num-split 20 --use-message-group

read  -n 1 -p "Press key to continue" mainmenuinput