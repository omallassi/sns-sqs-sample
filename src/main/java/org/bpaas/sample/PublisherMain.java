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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * - publish on two tenant
 * - one tenant has dedicated queues for each activity
 * - + will re-run a SoD
 * - will have xVA Fee impacted by PU
 * - the other tenant will have a single queue for everything and a single xVA Computation Stack behind
 */
public class PublisherMain {

    @Option(name = "--topic-arn", required = false)
    public static String snsTopic = "arn:aws:sns:us-east-1:427396468640:topic_all_tenant.fifo";

    @Option(name = "--tenant-id", required = false)
    public static String tenantId = "0";

    @Option(name = "--message-num", required = false)
    public static int messageNum = 1;

    @Option(name = "--message-num-split", required = false)
    public static int messageNumSplit = 10;

    @Option(name = "--use-message-group", required = false)
    public static boolean useMessageGroup = false;

    public static void main(String[] args) {
        PublisherMain publisher = new PublisherMain();
        OptionsParser.parseCommandLine(publisher, args);
        System.out.println("is Running on SNS [" + snsTopic + "] for tenant [" + tenantId + "] - message num [" + messageNum + "]");
        publisher.run();
    }

    public void run() {
        try {
            SnsClient publisher = SnsClient.builder().region(Region.US_EAST_1).credentialsProvider(ProfileCredentialsProvider.create()).build();

            String[] activities = new String[]{"risk-management", "xva-fee", "pricing"};
            Random random = new Random();

            //publish
            for (int i = 0; i < messageNum; i++) {
                String messageBody = "{pu: .... s3-ref: .......}";
                Map<String, MessageAttributeValue> messageAttrs = new HashMap<>();
                messageAttrs.put("tenant-id", MessageAttributeValue.builder().dataType("String").stringValue(tenantId).build()); //https://docs.aws.amazon.com/sns/latest/dg/sns-message-attributes.html
                messageAttrs.put("emit-ts", MessageAttributeValue.builder().dataType("String").stringValue( String.valueOf(System.currentTimeMillis())).build());

                int index = random.nextInt(0, 3);
                System.out.println("got index [" + index + "]");

                messageAttrs.put("activity", MessageAttributeValue.builder().dataType("String").stringValue(activities[index]).build());

//                if(i < messageNumSplit) {
//                    messageAttrs.put("activity", MessageAttributeValue.builder().dataType("String").stringValue("risk-management").build());
//                }
//                else{
//                    messageAttrs.put("activity", MessageAttributeValue.builder().dataType("String").stringValue("xva-fee").build());
//                }

                PublishRequest.Builder builder = PublishRequest.builder()
                        .message(messageBody)
                        //.messageStructure("json")
                        .messageAttributes(messageAttrs)
                        .topicArn(snsTopic)
                        .messageGroupId(UUID.randomUUID().toString())
                        .messageDeduplicationId(UUID.randomUUID().toString());

                if(useMessageGroup) {
                    builder.messageGroupId(messageAttrs.get("activity").stringValue());
                }

                PublishRequest request = builder.build();

                PublishResponse response = publisher.publish(request);
                System.out.println("sent message [" + response.messageId() + "] - status [" + response.sdkHttpResponse().statusCode() + "]");
            }
        } catch (SnsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}