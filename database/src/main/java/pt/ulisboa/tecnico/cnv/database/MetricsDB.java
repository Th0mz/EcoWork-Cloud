package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AcquireLockOptions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBLockClientOptions;
import com.amazonaws.services.dynamodbv2.CreateDynamoDBTableOptions;
import com.amazonaws.services.dynamodbv2.LockItem;
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
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
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

    private static final AmazonDynamoDBLockClient lockClient = new AmazonDynamoDBLockClient(
        AmazonDynamoDBLockClientOptions.builder(client, "lockTable")
                .withTimeUnit(TimeUnit.SECONDS)
                .withLeaseDuration(5L)
                .withHeartbeatPeriod(2L)
                .withCreateHeartbeatBackgroundThread(true)
                .build());
        

    private static DynamoDB dynamoDB = new DynamoDB(client);

    private static String tableName = "metrics-table";

    private static Map<String, Map<String, Double>> metrics = new HashMap<String, Map<String, Double>>();

    private static Map<String, List<AbstractMetricObj>> objsToSave = new HashMap<String, List<AbstractMetricObj>>();

    private static List<CompressObj> bmpImages = new ArrayList<CompressObj>();
    private static List<CompressObj> pngImages = new ArrayList<CompressObj>();
    private static List<CompressObj> jpgImages = new ArrayList<CompressObj>();

    //first index for army size, second index for round number
    private static Map<Integer,  Map<Integer, InsectWarObj>> samearmysize = new HashMap<Integer, Map<Integer, InsectWarObj>>();
    private static List<InsectWarObj> roundOneEqualArmy = new ArrayList<InsectWarObj>();
    private static List<InsectWarObj> differentArmySizeList = new ArrayList<InsectWarObj>();
    private static ArrayList<Double> perArmyRatio = new ArrayList<Double>(
            Arrays.asList(0.954545, 0.960315, 0.965033, 0.968942, 0.97222, 0.974998, 0.977374, 0.979422, 0.981202, 0.982757, 
            0.984126, 0.985336, 0.986412, 0.987373, 0.988234, 0.98901, 0.989711, 0.990347, 0.990925, 0.991452, 
            0.991935, 0.992377, 0.992784, 0.993159, 0.993506, 0.993827, 0.994124, 0.9944, 0.994658, 0.994897, 
            0.995121, 0.995331, 0.995527, 0.995711, 0.995884, 0.996047, 0.9962, 0.996345, 0.996481, 0.99661, 
            0.996732, 0.996847, 0.996956, 0.99706, 0.997159, 0.997252, 0.997342, 0.997426, 0.997507, 0.997584, 
            0.997658, 0.997728, 0.997795, 0.997859, 0.997921, 0.99798, 0.998036, 0.99809, 0.998142, 0.998191, 
            0.998239, 0.998285, 0.998329, 0.998372, 0.998452, 0.99849, 0.998526, 0.998561, 0.998595, 0.998628, 
            0.99866, 0.99869, 0.99872, 0.998748, 0.998776, 0.998803, 0.998828, 0.998853, 0.998878, 0.998901, 
            0.998924, 0.998946, 0.998967, 0.998988, 0.999008, 0.999027, 0.999046, 0.999065, 0.999082));
    private static ArrayList<Integer> perArmyRatioCount = new ArrayList<Integer>();

    private static Double storedLastPerRound = 300000.0;
    private static Double storedLastPerArmy = 1.0;
    private static Double insect111Value = 900502.0;
    

    public MetricsDB() {
        objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
        objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
        objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
    }

    public static void main(String[] args) throws Exception {
        MetricsDB.createDB();
        MetricsDB.saveMetric(new FoxRabbitObj(10, 3, 1, 1000));
        MetricsDB.saveMetric(new FoxRabbitObj(5, 3, 1, 500));
        MetricsDB.saveMetric(new FoxRabbitObj(3, 3, 1, 300));
        MetricsDB.saveMetric(new FoxRabbitObj(10, 2, 1, 1500));
        MetricsDB.saveMetric(new FoxRabbitObj(5, 2, 1, 750));
        MetricsDB.saveMetric(new FoxRabbitObj(3, 2, 1, 450));
        // MetricsDB.saveMetric(new CompressObj("bmp", "0.2", 1156, 1336336, 865569L));
        // MetricsDB.saveMetric(new CompressObj("bmp", "0.3", 1157, 1336336, 866164L));
        // MetricsDB.saveMetric(new CompressObj("bmp", "0.4", 1158, 1336336, 875353L));
        MetricsDB.saveMetric(new CompressObj("png", "0.2", 1156, 1336336, 865569L));
        MetricsDB.saveMetric(new CompressObj("png", "0.3", 1157, 1336336, 866164L));
        MetricsDB.saveMetric(new CompressObj("png", "0.4", 1158, 1336336, 875353L));
        MetricsDB.saveMetric(new CompressObj("jpeg", "0.2", 1156, 1336336, 865569L));
        MetricsDB.saveMetric(new CompressObj("jpeg", "0.3", 1157, 1336336, 866164L));
        MetricsDB.saveMetric(new CompressObj("jpeg", "0.4", 1158, 1336336, 875353L));
        
        MetricsDB.saveMetric(new CompressObj("bmp", "0.4", 5, 7, 290L));
        MetricsDB.saveMetric(new CompressObj("bmp", "0.5", 6, 8, 302L));
        MetricsDB.saveMetric(new CompressObj("bmp", "0.5", 7, 9, 340L));
        MetricsDB.saveMetric(new InsectWarObj(1, 10, 10, 1310000));
        MetricsDB.saveMetric(new InsectWarObj(2, 10, 10, 1510000));
        //MetricsDB.saveMetric(new CompressObj("bmp", "0.4", 5, 7, 290L));
        //MetricsDB.saveMetric(new CompressObj("bmp", "0.5", 6, 8, 302L));
        //MetricsDB.saveMetric(new CompressObj("bmp", "0.5", 7, 9, 340L));
        //MetricsDB.saveMetric(new InsectWarObj(1, 10, 10, 1310000));
        //MetricsDB.saveMetric(new InsectWarObj(2, 10, 10, 1510000));

        MetricsDB.saveMetric(new InsectWarObj(1, 3, 3, 6000));
        MetricsDB.saveMetric(new InsectWarObj(2, 3, 3, 306000));

        //MetricsDB.saveMetric(new InsectWarObj(1, 2, 2, 4000));
        //MetricsDB.saveMetric(new InsectWarObj(4, 2, 2, 1804000));

        MetricsDB.saveMetric(new InsectWarObj(1, 10, 11, 9603536));
        MetricsDB.saveMetric(new InsectWarObj(1, 10, 20, 9603536));


        updateAllMetrics();
        getCompressMetrics();
        getFoxRabbitMetrics();
        getInsectWarMetrics();
        //getPerArmyRatio();


        //MetricsDB.saveMetric(new FoxRabbitObj(10, 3, 1, 5000));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 3, 1, 53000));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 3, 1, 33000));
        //MetricsDB.saveMetric(new FoxRabbitObj(10, 2, 1, 6000));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 2, 1, 51400));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 2, 1, 31400));

        //updateAllMetrics();
    }

    public static void createDB() throws Exception {

        try {

            /////////////////////////////////////////////////////////
            //Create table to store metrics
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

            // Create table if it does not exist yet
            boolean createdNewTable = TableUtils.createTableIfNotExists(client, createTableRequest);
            
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(client, tableName);
            //////////////////////////////////////////////////////
            

            /////////////////////////////////////////////////////
            //Create table to use for locks
            /* CreateTableRequest createLockTableRequest = new CreateTableRequest().withTableName("lockTable")
                .withProvisionedThroughput(new ProvisionedThroughput()
                .withReadCapacityUnits(10L).withWriteCapacityUnits(10L));
            
            TableUtils.createTableIfNotExists(client, createLockTableRequest);
            TableUtils.waitUntilActive(client, "lockTable"); */

            try {
                CreateDynamoDBTableOptions options = CreateDynamoDBTableOptions
                .builder(client, new ProvisionedThroughput()
                .withReadCapacityUnits(10L).withWriteCapacityUnits(10L), 
                "lockTable").build();
            
                AmazonDynamoDBLockClient.createLockTableInDynamoDB(options);
            } catch (ResourceInUseException e) {

            }
            
            
            
            ////////////////////////////////////////////////////


            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = client.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);
            
            objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
            objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());

            //NEED TO INITIALIZE COUNT
            for(int i = 0; i < perArmyRatio.size(); i++) {
                perArmyRatioCount.add(1);
            }


            if(!createdNewTable) return;

            //Update initial metrics to the database
            client.putItem(CompressObj.generateRequest(tableName, "jpeg", 0.02, 14000.0, 1));
            client.putItem(CompressObj.generateRequest(tableName, "png", 0.8, 28000.0, 1));
            client.putItem(CompressObj.generateRequest(tableName, "bmp", 180.0, 10000.0, 1));

            client.putItem(InsectWarObj.generateRequest(tableName, 1, 1.0, 
                1, 300000.0));

            //for(int i = 0; i < perArmyRatio.size(); i++) {
            //    client.putItem(InsectWarObj.generateRatioRequest(tableName, i, perArmyRatio.get(i), 1));
            //}

            client.putItem(InsectWarObj.generateOneRatioRequest(tableName, perArmyRatio, perArmyRatioCount));



            client.putItem(FoxRabbitObj.generateRequest(tableName, 1, 1, 9000.0));
            client.putItem(FoxRabbitObj.generateRequest(tableName, 1, 2, 30000.0));
            client.putItem(FoxRabbitObj.generateRequest(tableName, 1, 3, 110000.0));
            client.putItem(FoxRabbitObj.generateRequest(tableName, 1, 4, 250000.0));

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

    public static void initialize() {
        for(int i = 0; i < perArmyRatio.size(); i++) {
            perArmyRatioCount.add(1);
        }
    }

    public static synchronized void saveMetric(String typeRequest, String argsRequest, Double value) {
        if(!typeRequest.equals("war") && !typeRequest.equals("foxrabbit") 
            && !typeRequest.equals("compression"))
            return;
        if(!metrics.containsKey(typeRequest)) {
            metrics.put(typeRequest, new HashMap<String, Double>());
        }
        metrics.get(typeRequest).put(argsRequest, value);
        System.out.println(String.format("TYPE OF REQUEST-%s | ARGS %s | NRINSTR-%d", typeRequest, argsRequest, value));
    }

    private static Map<String, AttributeValue> newItem(String typeRequest, String argsRequest, Double value) {
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("typeRequest", new AttributeValue(typeRequest));
        itemValues.put("argsRequest", new AttributeValue(argsRequest));
        itemValues.put("value", new AttributeValue().withN(Double.toString(value)));
        return itemValues;
    }

    public static void insertNewItem(String typeRequest, String argsRequest, Double value) {
        // Add an item
        client.putItem(new PutItemRequest(tableName, newItem(typeRequest, argsRequest, value)));
    }

    public synchronized static void uploadAllMetrics() {
        for (Map.Entry<String, Map<String, Double>> map : metrics.entrySet()) { //Iterate over all types of requests
            for (Map.Entry<String, Double> entry : map.getValue().entrySet()) { //Iterate over all arguments per type of request
                insertNewItem(map.getKey(), entry.getKey(), entry.getValue());
                System.out.println(String.format("TYPE-%s | ARGS-%s | NRINSTR-%d", 
                                    map.getKey(), entry.getKey(), entry.getValue()));
                System.out.println(map.getValue());
                System.out.println(entry.getKey());
            }
        }
        metrics.put("war", new HashMap<String, Double>());
        metrics.put("foxrabbit", new HashMap<String, Double>());
        metrics.put("compression", new HashMap<String, Double>());
    }



    public static void saveMetric(AbstractMetricObj obj) {
        if(obj instanceof FoxRabbitObj)
            objsToSave.get(FoxRabbitObj.endpoint).add((FoxRabbitObj)obj);

        else if(obj instanceof InsectWarObj) {
            objsToSave.get(InsectWarObj.endpoint).add((InsectWarObj)obj);
            InsectWarObj objC = (InsectWarObj) obj;
            if(objC.getArmy1() == objC.getArmy2()) {
                if(!samearmysize.containsKey(objC.getArmy1())) samearmysize.put(objC.getArmy1(), new HashMap<Integer, InsectWarObj>());
                samearmysize.get(objC.getArmy1()).put(objC.getMax(), objC);
                
                if(objC.getMax() == 1) {
                    roundOneEqualArmy.add(objC);
                }
            } else {
                differentArmySizeList.add(objC);
            }

        } else if(obj instanceof CompressObj) {
            System.out.println("Format: " + ((CompressObj) obj).getFormat());
            objsToSave.get(CompressObj.endpoint).add((CompressObj)obj);
            if(((CompressObj) obj).getFormat().equals("bmp")) {
                bmpImages.add((CompressObj) obj);
            } else if(((CompressObj) obj).getFormat().equals("png")) {
                pngImages.add((CompressObj) obj);
            } else if(((CompressObj) obj).getFormat().equals("jpeg")) {
                jpgImages.add((CompressObj) obj);
            } 

        } else {
            return;
        }
    }


    public static ScanResult getItemsForEndpoint(String endpoint) {
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.EQ.toString())
                .withAttributeValueList(new AttributeValue(endpoint));
            scanFilter.put("endpoint", condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = client.scan(scanRequest);
        return scanResult;
    }




 // =====================================================================
 // ========================                     ========================
 // ========================   UPDATE FUNCTIONS  ========================
 // ========================                     ========================
 // =====================================================================



    public static void updateAllMetrics() { 
        // for all AbstractMetricObj: generate new PutItemRequest; client.putItem();

        updateFoxesRabbits();
        if(bmpImages.size() >= 3) updateBMP();
        if(pngImages.size() >= 3) updatePNG();
        if(jpgImages.size() >= 3) updateJPG();
        updateInsectWars();
    }




    public static void updateInsectWars() {

        try {
            
            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder("war").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on UpdateWarBasic");

                if(!objsToSave.containsKey(InsectWarObj.endpoint)) {
                    objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
                    return;
                }

                Double round1perarmysize = 1.0, roundincreasewhenarmyequal = 300000.0;
                Integer nr_round1perarmysize = 1, nr_roundincreasewhenarmyequal = 1;


                ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint);
                List<Map<String,AttributeValue>> listItems = sr.getItems();

                for(Map<String,AttributeValue> itemAttributes : listItems) {
                    round1perarmysize = Double.parseDouble(itemAttributes.get("round1perarmysize").getN());
                    nr_round1perarmysize = Integer.parseInt(itemAttributes.get("nr_round1perarmysize").getN());

                    roundincreasewhenarmyequal = Double.parseDouble(itemAttributes.get("roundincreasewhenarmyequal").getN());
                    nr_roundincreasewhenarmyequal = Integer.parseInt(itemAttributes.get("nr_roundincreasewhenarmyequal").getN());
                }


                //Calculus of per round variation as a function of army size,
                // when armies are equal
                int nr_measures = 0;
                Double estimatePerRound = 0.0;
                //List<InsectWarObj> roundOneBaselines = new ArrayList<InsectWarObj>();
                for (Integer armySize : samearmysize.keySet()) {
                    if (samearmysize.get(armySize).size() > 1) {
                        List<Integer> toRemove = new ArrayList<Integer>();
                        for (InsectWarObj obj1 : samearmysize.get(armySize).values()) {
                            if (obj1.getMax() != 1) continue;
                            //roundOneBaselines.add(obj1);
                            for (InsectWarObj obj2 : samearmysize.get(armySize).values()) {
                                if (obj1.getMax() == obj2.getMax()) continue;
                                nr_measures++;
                                int roundDif = obj2.getMax() - obj1.getMax();
                                Long instrDif = obj2.getInstructions() - obj1.getInstructions();
                                Double functionOfRound = instrDif*1.0 / (armySize*roundDif*1.0);
                                System.out.println("GETS HERE!!!");
                                toRemove.add(obj2.getMax());
                                estimatePerRound += functionOfRound;
                            }
                        }
                        for(Integer key : toRemove) {
                            samearmysize.get(armySize).remove(key);
                        }
                    }
                }
                //first index for army size, second index for round number 
                /* samearmysize = new HashMap<Integer, Map<Integer, InsectWarObj>>();
                for(InsectWarObj obj : roundOneBaselines) {
                    if(!samearmysize.containsKey(obj.getArmy1()))
                        samearmysize.put(obj.getArmy1(), new HashMap<Integer, InsectWarObj>());
                    samearmysize.get(obj.getArmy1()).put(obj.getMax(), obj);
                } */


                //Calculus of per army size variation as a function of round,
                // when armies are equal and round is one
                int nr_measures_roundone = 0;
                Double estimatePerArmy = 0.0;
                for (InsectWarObj obj1 : roundOneEqualArmy) {
                    System.out.println("----------->>>>>-----------");
                    System.out.println("OBJ1 Army: " + obj1.getArmy1());

                    Double armyRatio =  obj1.getArmy1()*1.0/1.0; //army1 is the baseline for comparison
                    Double instrRatio = obj1.getInstructions()*1.0/insect111Value;
                    Double functionOfArmy = instrRatio/armyRatio;
                    estimatePerArmy += functionOfArmy;
                    nr_measures_roundone++;
                    System.out.println("Army ratio: " + armyRatio);
                    System.out.println("Instr ratio: " + instrRatio);
                    System.out.println("FunctionOfArmy: " + functionOfArmy);
                    System.out.println("EstimatePerArmy: " + estimatePerArmy);

                    System.out.println("-----------<<<<<-----------");
                }

                Integer nr_finalPerRound, nr_finalPerArmy;
                Double finalPerRound, finalPerArmy;

                if (estimatePerRound == 0) {
                    finalPerRound = roundincreasewhenarmyequal;
                    nr_finalPerRound = nr_roundincreasewhenarmyequal;
                    System.out.println(String.format("[INSECTWAR - DB] PERROUND-%f NR_INSTANCES-%d", finalPerRound, nr_finalPerRound));

                } else {
                    nr_finalPerRound = nr_measures + nr_roundincreasewhenarmyequal;
                    finalPerRound = (estimatePerRound + roundincreasewhenarmyequal * nr_roundincreasewhenarmyequal) / nr_finalPerRound;
                    System.out.println(String.format("[INSECTWAR - DB&LOCAL] PERROUND-%f NR_INSTANCES-%d", finalPerRound, nr_finalPerRound));

                }

                if (estimatePerArmy == 0) {
                    finalPerArmy = round1perarmysize;
                    nr_finalPerArmy = nr_round1perarmysize;
                    System.out.println(String.format("[INSECTWAR - DB] PERARMY-%f NR_INSTANCES-%d", finalPerArmy, nr_finalPerArmy));

                } else {
                    nr_finalPerArmy = nr_measures_roundone + nr_round1perarmysize;
                    finalPerArmy = (estimatePerArmy + round1perarmysize * nr_round1perarmysize) / nr_finalPerArmy;
                    System.out.println(String.format("[INSECTWAR - DB&LOCAL] PERARMY-%f NR_INSTANCES-%d", finalPerArmy, nr_finalPerArmy));
                    System.out.println(String.format("[INSECTWAR - DB&LOCAL *] estimatePerArmy-%f round1perarmysize-%f", estimatePerArmy, round1perarmysize));
                    System.out.println(String.format("[INSECTWAR - DB&LOCAL **] nr_round1perarmysize-%d nr_measures_roundone-%d", nr_round1perarmysize, nr_measures_roundone));
                }
            
                storedLastPerArmy = finalPerArmy;
                storedLastPerRound = finalPerRound;
                System.out.println(String.format("[INSECTWAR] NEW STATISTIC: PERROUND-%f PERARMY-%f", finalPerRound, finalPerArmy));
                System.out.println(String.format("PERROUND-%d", nr_finalPerRound));
                client.putItem(InsectWarObj.generateRequest(tableName, nr_finalPerArmy, finalPerArmy, 
                        nr_finalPerRound, finalPerRound));

                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on UpdateWarBasic");
            } else {
                System.out.println("Could not get lock");
            }
            
        } catch (Exception e ) {
            //e.printStackTrace();
        }



        try {
            final AmazonDynamoDBLockClient lockClient = new AmazonDynamoDBLockClient(
                AmazonDynamoDBLockClientOptions.builder(client, "lockTable")
                        .withTimeUnit(TimeUnit.SECONDS)
                        .withLeaseDuration(10L)
                        .withHeartbeatPeriod(3L)
                        .withCreateHeartbeatBackgroundThread(true)
                        .build());

            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder("warratio").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on UpdateWarRatio");

                //Also update the Ratios of instr/armyratio
                HashMap<Integer, Double> sumPerIndex = new HashMap<Integer, Double>();
                HashMap<Integer, Integer> countPerIndex = new HashMap<Integer, Integer>();
                
                for(InsectWarObj obj : differentArmySizeList) {
                    int army1 = obj.getArmy1();
                    int army2 = obj.getArmy2();
                    int round = obj.getMax();
                    System.out.println(String.format("Updating based on (%d, %d, %d)", round, army1, army2));
                    Double value = insect111Value;
                    if (army1==army2) continue;
                    if (army2 < army1) {
                        value = value * (storedLastPerArmy*army2);
                        value = value + storedLastPerRound * army2 * (round-1);
                        int index = (int) (((army1*1.0/army2) - 1) / 0.1) - 1;
                        if(index > 88) index = 88; //there are only 89 ratios stored, after that the change is irrelevant
                        Double calculatedRatio = obj.getInstructions()*1.0 / (value * (army1*1.0/army2));
                        //Double afterApplyingRatio = value * perArmyRatio.get(index) * (army1/army2);
                        System.out.println("Calculated ratio:" + calculatedRatio);

                        if(!sumPerIndex.containsKey(index)) sumPerIndex.put(index, 0.0);
                        sumPerIndex.put(index, sumPerIndex.get(index) + calculatedRatio);

                        if(!countPerIndex.containsKey(index)) countPerIndex.put(index, 0);
                        countPerIndex.put(index, countPerIndex.get(index) + 1);
                    } 
                    else { //army1 < army2
                        value = value * (storedLastPerArmy*army1);
                        value = value + storedLastPerRound * army1 * (round-1);
                        int index = (int) (((army2*1.0/army1) - 1) / 0.1) - 1;
                        if(index > 88) index = 88; //there are only 89 ratios stored, after that the change is irrelevant
                        //value = value * perArmyRatio.get(index) * (army2/army1);
                        Double calculatedRatio = obj.getInstructions()*1.0 / (value * (army2*1.0/army1));
                        System.out.println(String.format("Number instr: %d | previousStep: %f | ratio: %f", obj.getInstructions(), value, army2*1.0/army1));
                        System.out.println(String.format("Calculated ratio is %f (index %d)", calculatedRatio, index));
                        
                        if(!sumPerIndex.containsKey(index)) sumPerIndex.put(index, 0.0);
                        sumPerIndex.put(index, sumPerIndex.get(index) + calculatedRatio);

                        if(!countPerIndex.containsKey(index)) countPerIndex.put(index, 0);
                        countPerIndex.put(index, countPerIndex.get(index) + 1);

                    }
                }

                /* for(Integer index : countPerIndex.keySet()) {
                    ScanResult res = getItemsForEndpoint(InsectWarObj.endpoint + String.valueOf(index));
                    List<Map<String,AttributeValue>> listOfRatios = res.getItems();
                    System.out.println(String.format("Get items with key %s", InsectWarObj.endpoint + String.valueOf(index)));

                    Integer totalCount = countPerIndex.get(index);
                    Double totalSum = sumPerIndex.get(index);

                    for(Map<String,AttributeValue> itemAttributes : listOfRatios) {
                        Double storedRatio = Double.parseDouble(itemAttributes.get("perArmyRatio").getN());
                        Integer numberStored = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                        totalCount += numberStored;
                        totalSum += (storedRatio * numberStored);
                        System.out.println(String.format("storedRatio-%f numberStored-%d totalCount-%d totalSum-%f", 
                            storedRatio, numberStored, totalCount, totalSum));
                    }
                    Double totalRatio = totalSum / totalCount;
                    System.out.println(String.format("Total ratio-%f", totalRatio));
                    perArmyRatio.set(index, totalRatio);
                    perArmyRatioCount.set(index, totalCount);
                    client.putItem(InsectWarObj.generateRatioRequest(tableName, index, totalRatio, totalCount));
                } */

                ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint + "ratio");
                List<Map<String,AttributeValue>> listItems = sr.getItems();
                for(Map<String,AttributeValue> itemAttributes : listItems) {
                    for(int i = 0; i < perArmyRatio.size(); i++) {
                        Double r = Double.parseDouble(itemAttributes.get("perArmyRatio"+String.valueOf(i)).getN());
                        Integer c = Integer.parseInt(itemAttributes.get("nr_previous"+String.valueOf(i)).getN());
                        System.out.println("INSECT GOT PerArmyRatio-"+ r + " with count-" + c + " in index-" + i);
                        perArmyRatio.set(i, r);
                        perArmyRatioCount.set(i, c);
                    }
                }

                for(Integer index : countPerIndex.keySet()) {
                    
                    Integer totalCount = countPerIndex.get(index);
                    Double totalSum = sumPerIndex.get(index);

                    Double storedRatio = perArmyRatio.get(index);
                    Integer numberStored = perArmyRatioCount.get(index);
                    totalCount += numberStored;
                    totalSum += (storedRatio * numberStored);
                    System.out.println(String.format("storedRatio-%f numberStored-%d totalCount-%d totalSum-%f", 
                        storedRatio, numberStored, totalCount, totalSum));
                    
                    Double totalRatio = totalSum / totalCount;
                    System.out.println(String.format("Total ratio-%f", totalRatio));
                    perArmyRatio.set(index, totalRatio);
                    perArmyRatioCount.set(index, totalCount);
                    //client.putItem(InsectWarObj.generateRatioRequest(tableName, index, totalRatio, totalCount));
                }

                client.putItem(InsectWarObj.generateOneRatioRequest(tableName, perArmyRatio, perArmyRatioCount));

                differentArmySizeList.clear();

                objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
                roundOneEqualArmy.clear();
                
                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on UpdateWarRatio");
            } else {
                System.out.println("Could not get lock");
            }
            
        } catch (Exception e ) {
            //e.printStackTrace();
        }
    }




    public static void updateBMP() {

        try {
            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder("compressbmp").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on BMP");

                HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
                HashMap<String, Double> previousSlope = new HashMap<String, Double>();
                HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

                HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
                HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
                List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

                List<String> formats = Arrays.asList("bmp", "png", "jpeg"); 
                for(String t : formats) {
                    sumEachSlope.put(t, 0.0);
                    sumEachOrigin.put(t, 0.0);
                    totalMeasuresPerWorld.add(0);
                    previousSlope.put(t, 0.0);
                    previousOrigin.put(t, 0.0);
                    nr_previous.put(t, 0);
                }


                for(String f : formats) {
                    ScanResult sr = getItemsForEndpoint(CompressObj.endpoint+f);
                    List<Map<String,AttributeValue>> listItems = sr.getItems();
                    for(Map<String,AttributeValue> itemAttributes : listItems) {
                        String format = itemAttributes.get("format").getS();
                        Double previous_slope = Double.parseDouble(itemAttributes.get("slope").getN());
                        Double previous_origin = Double.parseDouble(itemAttributes.get("origin").getN());
                        int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                        nr_previous.put(format, previous_runs);
                        previousSlope.put(format, previous_slope);
                        previousOrigin.put(format, previous_origin);
                    }
                }

                List<Double> x = new ArrayList<Double>();
                List<Double> y = new ArrayList<Double>();

                int x_temp = bmpImages.get(0).getHeight();
                boolean enough_points = false;
                for(CompressObj obj : bmpImages) {
                    if(obj.getHeight() != x_temp) enough_points = true; 
                    x.add(obj.getHeight().doubleValue());
                    y.add(obj.getInstructions().doubleValue());
                }

                if (!enough_points) {
                    lockClient.releaseLock(lockItem.get());
                    System.out.println("RELEASED Lock on BMP");
                    return;
                } 

                LinearRegression regression = new LinearRegression(x, y);
                regression.calculateRegression();

                Double newSlope = regression.getSlope();
                Double newOrigin = regression.getOrigin();
                System.out.println("NEW SLOPE: " + newSlope);
                System.out.println("NEW ORIGIN: " + newOrigin);

                Integer numberMeasures = nr_previous.get("bmp")+bmpImages.size();
                Double finalSlope = (newSlope * bmpImages.size() + previousSlope.get("bmp") * nr_previous.get("bmp")) / numberMeasures;
                Double finalOrigin = (newOrigin * bmpImages.size() + previousOrigin.get("bmp") * nr_previous.get("bmp")) / numberMeasures;
                System.out.println("[COMPRESS - BMP] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
                client.putItem(CompressObj.generateRequest(tableName, "bmp", finalSlope, finalOrigin, numberMeasures));
                
                objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
                bmpImages.clear();
                
                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on BMP");
            } else {
                System.out.println("Could not get lock");
            }
            
        } catch (Exception e ) {
            //e.printStackTrace();
        }
    }



    public static void updateJPG() {

        try {
            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder("compressjpeg").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on JPG");

                HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
                HashMap<String, Double> previousSlope = new HashMap<String, Double>();
                HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

                HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
                HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
                List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

                List<String> formats = Arrays.asList("bmp", "png", "jpeg"); 
                for(String t : formats) {
                    sumEachSlope.put(t, 0.0);
                    sumEachOrigin.put(t, 0.0);
                    totalMeasuresPerWorld.add(0);
                    previousSlope.put(t, 0.0);
                    previousOrigin.put(t, 0.0);
                    nr_previous.put(t, 0);
                }


                for(String f : formats) {
                    ScanResult sr = getItemsForEndpoint(CompressObj.endpoint+f);
                    List<Map<String,AttributeValue>> listItems = sr.getItems();
                    for(Map<String,AttributeValue> itemAttributes : listItems) {
                        String format = itemAttributes.get("format").getS();
                        Double previous_slope = Double.parseDouble(itemAttributes.get("slope").getN());
                        Double previous_origin = Double.parseDouble(itemAttributes.get("origin").getN());
                        int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                        nr_previous.put(format, previous_runs);
                        previousSlope.put(format, previous_slope);
                        previousOrigin.put(format, previous_origin);
                    }
                }

                List<Double> x = new ArrayList<Double>();
                List<Double> y = new ArrayList<Double>();

                int x_temp = jpgImages.get(0).getPixels();
                boolean enough_points = false;
                for(CompressObj obj : jpgImages) {
                    if(obj.getPixels() != x_temp) enough_points = true;
                    x.add(obj.getPixels().doubleValue());
                    y.add(obj.getInstructions().doubleValue());
                }

                if(!enough_points){
                    lockClient.releaseLock(lockItem.get());
                    System.out.println("RELEASED Lock on JPG");
                    return;
                } 

                LinearRegression regression = new LinearRegression(x, y);
                regression.calculateRegression();

                Double newSlope = regression.getSlope();
                Double newOrigin = regression.getOrigin();

                Integer numberMeasures = nr_previous.get("jpeg")+jpgImages.size();
                Double finalSlope = (newSlope * jpgImages.size() + previousSlope.get("jpeg") * nr_previous.get("jpeg")) / numberMeasures;
                Double finalOrigin = (newOrigin * jpgImages.size() + previousOrigin.get("jpeg") * nr_previous.get("jpeg")) / numberMeasures;
                System.out.println("[COMPRESS - JPG] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
                client.putItem(CompressObj.generateRequest(tableName, "jpeg", finalSlope, finalOrigin, numberMeasures));
                
                objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
                jpgImages.clear();

                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on JPG");
            } else {
                System.out.println("Could not get lock");
            }
            
        } catch (Exception e ) {
            //e.printStackTrace();
        }
    }




    public static void updatePNG() {

        try {

            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder("compresspng").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on PNG");

                HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
                HashMap<String, Double> previousSlope = new HashMap<String, Double>();
                HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

                HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
                HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
                List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

                List<String> formats = Arrays.asList("bmp", "png", "jpeg"); 
                for(String t : formats) {
                    sumEachSlope.put(t, 0.0);
                    sumEachOrigin.put(t, 0.0);
                    totalMeasuresPerWorld.add(0);
                    previousSlope.put(t, 0.0);
                    previousOrigin.put(t, 0.0);
                    nr_previous.put(t, 0);
                }

                

                for(String f : formats) {
                    ScanResult sr = getItemsForEndpoint(CompressObj.endpoint+f);
                    List<Map<String,AttributeValue>> listItems = sr.getItems();
                    for(Map<String,AttributeValue> itemAttributes : listItems) {
                        String format = itemAttributes.get("format").getS();
                        Double previous_slope = Double.parseDouble(itemAttributes.get("slope").getN());
                        Double previous_origin = Double.parseDouble(itemAttributes.get("origin").getN());
                        int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                        nr_previous.put(format, previous_runs);
                        previousSlope.put(format, previous_slope);
                        previousOrigin.put(format, previous_origin);
                    }
                }

                List<Double> x = new ArrayList<Double>();
                List<Double> y = new ArrayList<Double>();

                int x_temp = pngImages.get(0).getPixels();
                boolean enough_points = false;
                for(CompressObj obj : pngImages) {
                    if(obj.getPixels() != x_temp) enough_points = true;
                    x.add(obj.getPixels().doubleValue());
                    y.add(obj.getInstructions().doubleValue());
                }
                if(!enough_points){
                    lockClient.releaseLock(lockItem.get());
                    System.out.println("RELEASED Lock on PNG");
                    return;
                } 

                LinearRegression regression = new LinearRegression(x, y);
                regression.calculateRegression();

                Double newSlope = regression.getSlope();
                Double newOrigin = regression.getOrigin();
                System.out.println("NEW SLOPE: " + newSlope);
                System.out.println("NEW ORIGIN: " + newOrigin);

                Integer numberMeasures = nr_previous.get("png")+pngImages.size();
                Double finalSlope = (newSlope * pngImages.size() + previousSlope.get("png") * nr_previous.get("png")) / numberMeasures;
                Double finalOrigin = (newOrigin * pngImages.size() + previousOrigin.get("png") * nr_previous.get("png")) / numberMeasures;
                System.out.println("[COMPRESS - PNG] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
                client.putItem(CompressObj.generateRequest(tableName, "png", finalSlope, finalOrigin, numberMeasures));
                
                objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
                pngImages.clear();
                
                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on PNG");
            } else {
                System.out.println("Could not get lock");
            }
            
        } catch (Exception e ) {
            //e.printStackTrace();
        }
    }




    public static void updateFoxesRabbits() {

        try {

            final Optional<LockItem> lockItem =
                    Optional.ofNullable(lockClient.acquireLock(AcquireLockOptions.builder(FoxRabbitObj.endpoint+"1").build()));
            
            if (lockItem.isPresent()) {
                System.out.println("GOT Lock on FoxesRabbits");
                if(!objsToSave.containsKey(FoxRabbitObj.endpoint)) {
                    objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
                    lockClient.releaseLock(lockItem.get());
                    System.out.println("RELEASED Lock on FoxesRabbits");
                    return;
                }

                HashMap<Integer, Integer> nr_previous = new HashMap<Integer, Integer>();
                HashMap<Integer, Double> previousMetric = new HashMap<Integer, Double>();
                HashMap<Integer, Double> sumEachWorld = new HashMap<Integer, Double>();
                HashMap<Integer, Integer> totalMeasuresPerWorld = new HashMap<Integer, Integer>();
                for(int n_world = 1; n_world <= 4; n_world++) {
                    sumEachWorld.put(n_world, 0.0);
                    totalMeasuresPerWorld.put(n_world, 0);
                    previousMetric.put(n_world, 0.0);
                    nr_previous.put(n_world, 0);
                }

                for(int n_world = 1; n_world <= 4; n_world++) {
                    ScanResult sr = getItemsForEndpoint(FoxRabbitObj.endpoint + String.valueOf(n_world));
                    List<Map<String,AttributeValue>> listItems = sr.getItems();

                    for(Map<String,AttributeValue> itemAttributes : listItems) {
                        int wrld = Integer.parseInt(itemAttributes.get("world").getN());
                        int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                        Double previous_metric = Double.parseDouble(itemAttributes.get("statistic").getN());
                        nr_previous.put(wrld, previous_runs);
                        previousMetric.put(wrld, previous_metric);
                    }
                }

                for(AbstractMetricObj obj : objsToSave.get(FoxRabbitObj.endpoint)) {
                    if (obj instanceof FoxRabbitObj) {
                        FoxRabbitObj fR = (FoxRabbitObj) obj;
                        sumEachWorld.put(fR.getWorld(), sumEachWorld.get(fR.getWorld()) + fR.getWeight());
                        totalMeasuresPerWorld.put(fR.getWorld(), totalMeasuresPerWorld.get(fR.getWorld())+1);
                    }
                }

                for(int n_world = 1; n_world <= 4; n_world++) {
                    //System.out.println(String.format("============= [FOXRABBIT - WORLD] ==============="));
                    //System.out.println(String.format("nr_previous = %d", nr_previous.get(n_world)));
                    //System.out.println(String.format("totalMeasuresPerWorld = %d", totalMeasuresPerWorld.get(n_world)));

                    Integer numberMeasures = nr_previous.get(n_world)+totalMeasuresPerWorld.get(n_world);
                    //System.out.println(String.format("numberMeasures = %d", numberMeasures));

                    int actualMeasures = numberMeasures;
                    if (numberMeasures == 0) numberMeasures = 1;
                    Double finalStat = (sumEachWorld.get(n_world) + previousMetric.get(n_world) * nr_previous.get(n_world)) / numberMeasures;
                    //System.out.println(String.format("sumEachWorld = %d", sumEachWorld.get(n_world)));
                    //System.out.println(String.format("previousMetric = %d", previousMetric.get(n_world)));
                    //System.out.println(String.format("nr_previous = %d", nr_previous.get(n_world)));
        
                    //System.out.println("[FOXRABBIT - WORLD "+ n_world + "]NEW STATISTIC "+ finalStat);
                    client.putItem(FoxRabbitObj.generateRequest(tableName, actualMeasures, n_world, finalStat));
                }

                objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
                
                lockClient.releaseLock(lockItem.get());
                System.out.println("RELEASED Lock on FoxesRabbits");
            } else {
                System.out.println("Could not get lock");
            }
    
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

 // =====================================================================
 // ==========================                 ==========================
 // ==========================   GET METRICS   ==========================
 // ==========================                 ==========================
 // =====================================================================

    public static HashMap<Integer, Double> getFoxRabbitMetrics() {
        HashMap<Integer,Double> metricsPerWorld = new HashMap<Integer, Double>();

        for(int n_world = 1; n_world <= 4; n_world++) {
            metricsPerWorld.put(n_world, 0.0);
            ScanResult sr = getItemsForEndpoint(FoxRabbitObj.endpoint + String.valueOf(n_world));
            List<Map<String,AttributeValue>> listItems = sr.getItems();
            for(Map<String,AttributeValue> itemAttributes : listItems) {
                int wrld = Integer.parseInt(itemAttributes.get("world").getN());
                Double previous_metric = Double.parseDouble(itemAttributes.get("statistic").getN());
                System.out.println("FOXES GOT: " + previous_metric);
                metricsPerWorld.put(wrld, previous_metric);
            } 
        }
        
        return metricsPerWorld;
    }
    


    public static HashMap<String, List<Double>> getCompressMetrics() {
        HashMap<String,List<Double>> metricsPerFormat = new HashMap<String, List<Double>>();
        
        metricsPerFormat.put("bmp", new ArrayList<Double>(List.of(0.0,0.0)));
        metricsPerFormat.put("jpeg", new ArrayList<Double>(List.of(0.0,0.0))); 
        metricsPerFormat.put("png", new ArrayList<Double>(List.of(0.0,0.0))); 

        List<String> formats = Arrays.asList("bmp", "png", "jpeg");

        for(String f : formats) {
            ScanResult sr = getItemsForEndpoint(CompressObj.endpoint + f);
            List<Map<String,AttributeValue>> listItems = sr.getItems();
            for(Map<String,AttributeValue> itemAttributes : listItems) {
                String format = itemAttributes.get("format").getS();
                Double previous_slope = Double.parseDouble(itemAttributes.get("slope").getN());
                Double previous_origin = Double.parseDouble(itemAttributes.get("origin").getN());
                System.out.println("COMPRESS GOT format-" + format + " slope-"+ previous_slope + "origin-"+previous_origin);

                metricsPerFormat.get(format).add(0, previous_slope);
                metricsPerFormat.get(format).add(1, previous_origin);
            }
        }
        

        return metricsPerFormat;
    }

    public static ArrayList<Double> getInsectWarMetrics() {

        //Index 0 - PerRoundArmyEqual
        //Index 1 - PerArmyRound1
        ArrayList<Double> metrics = new ArrayList<Double>();

        ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        //basic default value if there is no metric yet
        metrics.add(300000.0);
        metrics.add(1.0);

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            Double previous_perround = Double.parseDouble(itemAttributes.get("roundincreasewhenarmyequal").getN());
            Double previous_perarmy = Double.parseDouble(itemAttributes.get("round1perarmysize").getN());
            System.out.println("INSECT GOT Previous PerRound-"+ previous_perround + " PerArmy-" + previous_perarmy);
            metrics.set(0, previous_perround);
            metrics.set(1, previous_perarmy);
        }

        return metrics;
    }

    public static ArrayList<Double> getPerArmyRatio() {
        /* for(int i = 0; i < perArmyRatio.size(); i++) {
            ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint + String.valueOf(i));
            List<Map<String,AttributeValue>> listItems = sr.getItems();

            for(Map<String,AttributeValue> itemAttributes : listItems) {
                Double r = Double.parseDouble(itemAttributes.get("perArmyRatio").getN());
                System.out.println("INSECT GOT PerArmyRatio-"+ r + " in index-" + i);
                perArmyRatio.add(i, r);
            }
        } */

        ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint + "ratio");
        List<Map<String,AttributeValue>> listItems = sr.getItems();
        for(Map<String,AttributeValue> itemAttributes : listItems) {
            for(int i = 0; i < perArmyRatio.size(); i++) {
                Double r = Double.parseDouble(itemAttributes.get("perArmyRatio"+String.valueOf(i)).getN());
                Integer c = Integer.parseInt(itemAttributes.get("nr_previous"+String.valueOf(i)).getN());
                System.out.println("INSECT GOT PerArmyRatio-"+ r + " with count-" + c + " in index-" + i);
                perArmyRatio.set(i, r);
                perArmyRatioCount.set(i, c);
            }
        }

        return perArmyRatio;
    }





}
