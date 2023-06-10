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
import com.amazonaws.services.dynamodbv2.xspec.N;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;

/**
 * This sample demonstrates how to perform a few simple operations with the
 * Amazon DynamoDB service.
 */
public class MetricsDB {

    private static String AWS_REGION = "us-east-1";

    private static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(new EnvironmentVariableCredentialsProvider())
        .withRegion(AWS_REGION)
        .build();

    private static DynamoDB dynamoDB = new DynamoDB(client);

    private static String tableName = "metrics-table";

    private static Map<String, Map<String, Long>> metrics = new HashMap<String, Map<String, Long>>();

    private static List<AbstractMetricObj> objsToSave = new ArrayList<AbstractMetricObj>();

    public static void main(String[] args) throws Exception {
        //TODO: Use this main for testing of solely the DB
    }

    public static void createDB() throws Exception {

        try {

            ArrayList<AttributeDefinition> attrs = new ArrayList<AttributeDefinition>();
            attrs.add(new AttributeDefinition().withAttributeName("endpoint")
                    .withAttributeType("S"));

            ArrayList<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
            keySchema.add(new KeySchemaElement().withAttributeName("endpoint").withKeyType(KeyType.HASH));

            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(keySchema)
                .withProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(1L).withWriteCapacityUnits(1L));
            
            createTableRequest.setAttributeDefinitions(attrs);

            /* Table table = dynamoDB.createTable(createTableRequest);
            table.waitForActive(); */
            // Create table if it does not exist yet
            TableUtils.createTableIfNotExists(client, createTableRequest);
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(client, tableName);



            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = client.describeTable(describeTableRequest).getTable();
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

    public static synchronized void saveMetric(String typeRequest, String argsRequest, long value) {
        if(!typeRequest.equals("war") && !typeRequest.equals("foxrabbit") 
            && !typeRequest.equals("compression"))
            return;
        if(!metrics.containsKey(typeRequest)) {
            metrics.put(typeRequest, new HashMap<String, Long>());
        }
        metrics.get(typeRequest).put(argsRequest, value);
        System.out.println(String.format("TYPE OF REQUEST-%s | ARGS %s | NRINSTR-%d", typeRequest, argsRequest, value));
    }

    private static Map<String, AttributeValue> newItem(String typeRequest, String argsRequest, Long value) {
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("typeRequest", new AttributeValue(typeRequest));
        itemValues.put("argsRequest", new AttributeValue(argsRequest));
        itemValues.put("value", new AttributeValue().withN(Long.toString(value)));
        return itemValues;
    }

    public static void insertNewItem(String typeRequest, String argsRequest, Long value) {
        // Add an item
        client.putItem(new PutItemRequest(tableName, newItem(typeRequest, argsRequest, value)));
    }

    public synchronized static void uploadAllMetrics() {
        for (Map.Entry<String, Map<String, Long>> map : metrics.entrySet()) { //Iterate over all types of requests
            for (Map.Entry<String, Long> entry : map.getValue().entrySet()) { //Iterate over all arguments per type of request
                insertNewItem(map.getKey(), entry.getKey(), entry.getValue());
                System.out.println(String.format("TYPE-%s | ARGS-%s | NRINSTR-%d", 
                                    map.getKey(), entry.getKey(), entry.getValue()));
                System.out.println(map.getValue());
                System.out.println(entry.getKey());
            }
        }
        metrics.put("war", new HashMap<String, Long>());
        metrics.put("foxrabbit", new HashMap<String, Long>());
        metrics.put("compression", new HashMap<String, Long>());
    }

    public static void getAllItems() {
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = client.scan(scanRequest);
        System.out.println("Result: " + scanResult);
    }

    public static void updateAllMetrics() { 
        // for all AbstractMetricObj: generate new PutItemRequest; client.putItem();

        //DynamoLock()
        updateFoxesRabbits();
        //Do for the other two endpoints
        //DynamoUnlock()
    }

    public static void updateFoxesRabbits() {
        //TODO:
        //Read previous Fox metric
        HashMap<Integer, Integer> nr_previous = new HashMap<Integer, Integer>();
        HashMap<Integer, Long> previousMetric = new HashMap<Integer, Long>();
        HashMap<Integer, Long> sumEachWorld = new HashMap<Integer, Long>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();
        for(int n_world = 1; n_world <= 4; n_world++) {
            sumEachWorld.put(n_world, 0L);
            totalMeasuresPerWorld.add(0);
            previousMetric.put(n_world, 0L);
            nr_previous.put(n_world, 0);
        }

        for(AbstractMetricObj obj : objsToSave) {
            if (obj instanceof FoxRabbitObj) {
                FoxRabbitObj fR = (FoxRabbitObj) obj;
                sumEachWorld.put(fR.getWorld(), sumEachWorld.get(fR.getWorld()) + fR.getWeight());
            }
        }

        for(int n_world = 1; n_world <= 4; n_world++) {
            Integer numberMeasures = nr_previous.get(n_world)+totalMeasuresPerWorld.get(n_world-1);
            Long finalStat = (sumEachWorld.get(n_world) + previousMetric.get(n_world)
                             * nr_previous.get(n_world)) / numberMeasures;
            client.putItem(FoxRabbitObj.generateRequest(tableName, numberMeasures, n_world, finalStat));
        }
    }
    

}
