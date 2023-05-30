package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class MetricsDB {

    private static String AWS_REGION = "us-east-1";

    private static AmazonDynamoDB dynamoDB;

    private static String tableName = "metrics-table";

    public static void main(String[] args) throws Exception {

    }

    public static void createDB() throws Exception {
        dynamoDB = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .withRegion(AWS_REGION)
        .build();

        try {

            // Create a table with a primary hash key named 'name', which holds a string
            List<KeySchemaElement> key = new ArrayList<KeySchemaElement>();
            KeySchemaElement keySchemaElementP = new KeySchemaElement()
                .withAttributeName("typeRequest").withKeyType(KeyType.HASH);
            KeySchemaElement keySchemaElementS = new KeySchemaElement()
                .withAttributeName("argsRequest").withKeyType(KeyType.RANGE);
            key.add(keySchemaElementP);
            key.add(keySchemaElementS);

            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(key)
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("typeRequest").withAttributeType(ScalarAttributeType.S))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("argsRequest").withAttributeType(ScalarAttributeType.S))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("nrInstructions").withAttributeType(ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            // Create table if it does not exist yet
            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, tableName);

            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }

    public static synchronized void saveMetric(String typeRequest, String argsRequest, long metrics) {
        System.out.println(String.format("TYPE OF REQUEST-%s | ARGS %s | NRINSTR-%d", typeRequest, argsRequest, metrics));
    }

    private static Map<String, AttributeValue> newItem(String typeRequest, String argsRequest, int value) {
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("typeRequest", new AttributeValue(typeRequest));
        itemValues.put("argsRequest", new AttributeValue(argsRequest));
        itemValues.put("value", new AttributeValue().withN(Integer.toString(value)));
        return itemValues;
    }

    public static void insertNewItem() {
        // Add an item
        dynamoDB.putItem(new PutItemRequest(tableName, newItem("war", "1000:10:10", 1000)));
    }

    public static ScanResult getAllItems() {
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDB.scan(scanRequest);
        System.out.println("Result: " + scanResult);
        return scanResult;
    }

    
    

}
