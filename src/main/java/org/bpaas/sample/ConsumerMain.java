package org.bpaas.sample;

import org.bpaas.sample.helper.OptionsParser;
import org.kohsuke.args4j.Option;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.*;

/**
 * - publish on two tenant
 * - one tenant has dedicated queues for each activity
 * - + will re-run a SoD
 * - will have xVA Fee impacted by PU
 * - the other tenant will have a single queue for everything and a single xVA Computation Stack behind
 */
public class ConsumerMain {

    @Option(name = "--queue-url", required = false)
    public static String queueUrl = "https://sqs.us-east-1.amazonaws.com/427396468640/all_queue.fifo";

    @Option(name = "--max-num-msg", required = false)
    public static int maxNumberOfMessage = 10;


    public static void main(String[] args) {
        ConsumerMain consumer = new ConsumerMain();
        OptionsParser.parseCommandLine(consumer, args);
        System.out.println("is Running on SQS [" + queueUrl + "]");
        while(true) {
            consumer.run();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //do nada
            }
        }
    }

    public void run() {
        try {
            SqsClient client = SqsClient.builder()
                    .region(Region.US_EAST_1)
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .build();


            while (true) {
                ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxNumberOfMessage)
                        .messageAttributeNames(".*") //https://stackoverflow.com/questions/44238656/how-to-add-sqs-message-attributes-in-sns-subscription
                        .build();

                List<Message> messages = client.receiveMessage(request).messages();
                System.out.println("got [" + messages.size() + "] messages");
                messages.stream().forEach(m -> {
                    long emitTimestamp = Long.parseLong(m.messageAttributes().get("emit-ts").stringValue());
                    long currentTimestamp = System.currentTimeMillis();
                    long elapsed = currentTimestamp - emitTimestamp;

                    System.out.println("message: id [" + m.messageId() + "] - tenant-id [" + m.messageAttributes().get("tenant-id").stringValue() + "] - activity [" + m.messageAttributes().get("activity").stringValue() + "] - elapsed : [" + elapsed + "]");
                    //create some randomness to mimic "real" processing
                    Random random = new Random();
                    try {
                        Thread.sleep(random.nextInt(20) * 100);
                    } catch (InterruptedException e) {
                        System.err.println("error while thread sleeping");
                    }

                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build();
                    client.deleteMessage(deleteRequest);
                });
                if (messages.size() == 0)
                    break;
            }

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}