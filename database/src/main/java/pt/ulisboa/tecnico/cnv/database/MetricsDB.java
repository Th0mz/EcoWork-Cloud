package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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

    private static Map<String, List<AbstractMetricObj>> objsToSave = new HashMap<String, List<AbstractMetricObj>>();

    private static List<CompressObj> bmpImages = new ArrayList<CompressObj>();
    private static List<CompressObj> pngImages = new ArrayList<CompressObj>();
    private static List<CompressObj> jpgImages = new ArrayList<CompressObj>();

    //first index for army size, second index for round number
    private static Map<Integer,  Map<Integer, InsectWarObj>> samearmysize = new HashMap<Integer, Map<Integer, InsectWarObj>>();
    private static List<InsectWarObj> roundOneEqualArmy = new ArrayList<InsectWarObj>();
    

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

        updateAllMetrics();

        MetricsDB.saveMetric(new FoxRabbitObj(10, 3, 1, 5000));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 3, 1, 53000));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 3, 1, 33000));
        MetricsDB.saveMetric(new FoxRabbitObj(10, 2, 1, 6000));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 2, 1, 51400));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 2, 1, 31400));

        updateAllMetrics();
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

            objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
            objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());

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
            }

        } else if(obj instanceof CompressObj) {
            objsToSave.get(CompressObj.endpoint).add((CompressObj)obj);
            if(((CompressObj) obj).getFormat() == "bmp") {
                bmpImages.add((CompressObj) obj);
            } else if(((CompressObj) obj).getFormat() == "png") {
                pngImages.add((CompressObj) obj);
            } else if(((CompressObj) obj).getFormat() == "jpg") {
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

        //TODO: LOCK DB
        //DynamoLock()
        updateFoxesRabbits();
        if(bmpImages.size() > 5) updateBMP();
        if(pngImages.size() > 5) updatePNG();
        if(jpgImages.size() > 5) updateJPG();
        updateInsectWars();
        //DynamoUnlock()
    }




    public static void updateInsectWars() {
        if(!objsToSave.containsKey(InsectWarObj.endpoint)) {
            objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        Long round1perarmysize = 1L, roundincreasewhenarmyequal = 300000L;
        Integer nr_round1perarmysize = 1, nr_roundincreasewhenarmyequal = 1;


        ScanResult sr = getItemsForEndpoint(InsectWarObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            round1perarmysize = Long.parseLong(itemAttributes.get("round1perarmysize").getN());
            nr_round1perarmysize = Integer.parseInt(itemAttributes.get("nr_round1perarmysize").getN());

            roundincreasewhenarmyequal = Long.parseLong(itemAttributes.get("roundincreasewhenarmyequal").getN());
            nr_roundincreasewhenarmyequal = Integer.parseInt(itemAttributes.get("nr_roundincreasewhenarmyequal").getN());
        }


        //Calculus of per round variation as a function of army size,
        // when armies are equal
        int nr_measures = 0;
        Long estimatePerRound = 0L;
        for (Integer armySize : samearmysize.keySet()) {
            if (samearmysize.get(armySize).size() > 1) {
                for (InsectWarObj obj1 : samearmysize.get(listItems).values()) {
                    for (InsectWarObj obj2 : samearmysize.get(listItems).values()) {
                        if (obj1.getMax() == obj2.getMax()) continue;
                        nr_measures++;
                        int roundDif = Math.abs(obj2.getMax() - obj1.getMax());
                        long instrDif = Math.abs(obj2.getInstructions() - obj1.getInstructions());
                        long functionOfRound = instrDif / (armySize*roundDif);
                        estimatePerRound += functionOfRound;
                    }
                }
            }
        }
        if (nr_measures == 0) nr_measures = 1;
        estimatePerRound /= nr_measures; 


        //Calculus of per army size variation as a function of round,
        // when armies are equal and round is one
        int nr_measures_roundone = 0;
        Long estimatePerArmy = 0L;
        for (InsectWarObj obj1 : roundOneEqualArmy) {
            for (InsectWarObj obj2 : roundOneEqualArmy) {
                if (obj1.getArmy1() == obj2.getArmy1()) continue;
                nr_measures_roundone++;
                int armyDif = Math.abs(obj2.getArmy1() - obj1.getArmy1());
                long instrDif = Math.abs(obj2.getInstructions() - obj1.getInstructions());
                long functionOfArmy = instrDif / armyDif;
                estimatePerArmy += functionOfArmy;
            }
        }
        if (nr_measures_roundone == 0) nr_measures_roundone = 1;
        estimatePerArmy /= nr_measures_roundone; 

        Integer nr_finalPerRound, nr_finalPerArmy;
        Long finalPerRound, finalPerArmy;

        if (estimatePerRound == 0) {
            finalPerRound = roundincreasewhenarmyequal;
            nr_finalPerRound = nr_roundincreasewhenarmyequal;
        } else {
            nr_finalPerRound = nr_measures + nr_roundincreasewhenarmyequal;
            finalPerRound = (estimatePerRound * nr_measures + 
                    roundincreasewhenarmyequal * nr_roundincreasewhenarmyequal) / nr_finalPerRound;
        }

        if (estimatePerArmy == 0) {
            finalPerArmy = round1perarmysize;
            nr_finalPerArmy = nr_round1perarmysize;
        } else {
            nr_finalPerArmy = nr_measures_roundone + nr_round1perarmysize;
            finalPerArmy = (estimatePerArmy * nr_measures_roundone + 
                    round1perarmysize * nr_round1perarmysize) / nr_finalPerArmy;
            
        }
    
        System.out.println(String.format("[INSECTWAR] NEW STATISTIC: PERROUND-%d PERARMY-%d", finalPerRound, finalPerArmy));
        client.putItem(InsectWarObj.generateRequest(tableName, nr_finalPerArmy, finalPerArmy, 
                nr_finalPerRound, finalPerRound));

        objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
    }




    public static void updateBMP() {
        if(!objsToSave.containsKey(CompressObj.endpoint)) {
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
        HashMap<String, Long> previousSlope = new HashMap<String, Long>();
        HashMap<String, Long> previousOrigin = new HashMap<String, Long>();

        HashMap<String, Long> sumEachSlope = new HashMap<String, Long>();
        HashMap<String, Long> sumEachOrigin = new HashMap<String, Long>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
        for(String t : formats) {
            sumEachSlope.put(t, 0L);
            sumEachOrigin.put(t, 0L);
            totalMeasuresPerWorld.add(0);
            previousSlope.put(t, 0L);
            previousOrigin.put(t, 0L);
            nr_previous.put(t, 0);
        }

        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            String format = itemAttributes.get("format").getS();
            Long previous_slope = Long.parseLong(itemAttributes.get("slope").getN());
            Long previous_origin = Long.parseLong(itemAttributes.get("origin").getN());
            int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
            nr_previous.put(format, previous_runs);
            previousSlope.put(format, previous_slope);
            previousOrigin.put(format, previous_origin);
        }

        List<Double> x = new ArrayList<Double>();
        List<Double> y = new ArrayList<Double>();

        for(CompressObj obj : bmpImages) {
            x.add(obj.getHeight().doubleValue());
            y.add(obj.getInstructions().doubleValue());
        }

        LinearRegression regression = new LinearRegression(x, y);

        Double newSlope = regression.slope();
        Double newOrigin = regression.intercept();

        Integer numberMeasures = nr_previous.get("bmp")+bmpImages.size();
        Double finalSlope = (newSlope * bmpImages.size() + previousSlope.get("bmp") * nr_previous.get("bmp")) / numberMeasures;
        Double finalOrigin = (newOrigin * bmpImages.size() + previousOrigin.get("bmp") * nr_previous.get("bmp")) / numberMeasures;
        System.out.println("[COMPRESS - BMP] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
        client.putItem(CompressObj.generateRequest(tableName, "bmp", finalSlope, finalOrigin, numberMeasures));
        
        objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
        bmpImages.clear();
    }



    public static void updateJPG() {
        if(!objsToSave.containsKey(CompressObj.endpoint)) {
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
        HashMap<String, Long> previousSlope = new HashMap<String, Long>();
        HashMap<String, Long> previousOrigin = new HashMap<String, Long>();

        HashMap<String, Long> sumEachSlope = new HashMap<String, Long>();
        HashMap<String, Long> sumEachOrigin = new HashMap<String, Long>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
        for(String t : formats) {
            sumEachSlope.put(t, 0L);
            sumEachOrigin.put(t, 0L);
            totalMeasuresPerWorld.add(0);
            previousSlope.put(t, 0L);
            previousOrigin.put(t, 0L);
            nr_previous.put(t, 0);
        }

        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            String format = itemAttributes.get("format").getS();
            Long previous_slope = Long.parseLong(itemAttributes.get("slope").getN());
            Long previous_origin = Long.parseLong(itemAttributes.get("origin").getN());
            int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
            nr_previous.put(format, previous_runs);
            previousSlope.put(format, previous_slope);
            previousOrigin.put(format, previous_origin);
        }

        List<Double> x = new ArrayList<Double>();
        List<Double> y = new ArrayList<Double>();

        for(CompressObj obj : jpgImages) {
            x.add(obj.getPixels().doubleValue());
            y.add(obj.getInstructions().doubleValue());
        }

        LinearRegression regression = new LinearRegression(x, y);

        Double newSlope = regression.slope();
        Double newOrigin = regression.intercept();

        Integer numberMeasures = nr_previous.get("jpg")+jpgImages.size();
        Double finalSlope = (newSlope * jpgImages.size() + previousSlope.get("jpg") * nr_previous.get("jpg")) / numberMeasures;
        Double finalOrigin = (newOrigin * jpgImages.size() + previousOrigin.get("jpg") * nr_previous.get("jpg")) / numberMeasures;
        System.out.println("[COMPRESS - JPG] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
        client.putItem(CompressObj.generateRequest(tableName, "jpg", finalSlope, finalOrigin, numberMeasures));
        
        objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
        jpgImages.clear();
    }




    public static void updatePNG() {
        if(!objsToSave.containsKey(CompressObj.endpoint)) {
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
        HashMap<String, Long> previousSlope = new HashMap<String, Long>();
        HashMap<String, Long> previousOrigin = new HashMap<String, Long>();

        HashMap<String, Long> sumEachSlope = new HashMap<String, Long>();
        HashMap<String, Long> sumEachOrigin = new HashMap<String, Long>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
        for(String t : formats) {
            sumEachSlope.put(t, 0L);
            sumEachOrigin.put(t, 0L);
            totalMeasuresPerWorld.add(0);
            previousSlope.put(t, 0L);
            previousOrigin.put(t, 0L);
            nr_previous.put(t, 0);
        }

        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            String format = itemAttributes.get("format").getS();
            Long previous_slope = Long.parseLong(itemAttributes.get("slope").getN());
            Long previous_origin = Long.parseLong(itemAttributes.get("origin").getN());
            int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
            nr_previous.put(format, previous_runs);
            previousSlope.put(format, previous_slope);
            previousOrigin.put(format, previous_origin);
        }

        List<Double> x = new ArrayList<Double>();
        List<Double> y = new ArrayList<Double>();

        for(CompressObj obj : pngImages) {
            x.add(obj.getPixels().doubleValue());
            y.add(obj.getInstructions().doubleValue());
        }

        LinearRegression regression = new LinearRegression(x, y);

        Double newSlope = regression.slope();
        Double newOrigin = regression.intercept();

        Integer numberMeasures = nr_previous.get("png")+pngImages.size();
        Double finalSlope = (newSlope * pngImages.size() + previousSlope.get("png") * nr_previous.get("png")) / numberMeasures;
        Double finalOrigin = (newOrigin * pngImages.size() + previousOrigin.get("png") * nr_previous.get("png")) / numberMeasures;
        System.out.println("[COMPRESS - PNG] NEW SLOPE "+ finalSlope + " NEW INTERCEPT " + finalOrigin);
        client.putItem(CompressObj.generateRequest(tableName, "png", finalSlope, finalOrigin, numberMeasures));
        
        objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
        pngImages.clear();
    }




    public static void updateFoxesRabbits() {
        
        if(!objsToSave.containsKey(FoxRabbitObj.endpoint)) {
            objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        HashMap<Integer, Integer> nr_previous = new HashMap<Integer, Integer>();
        HashMap<Integer, Long> previousMetric = new HashMap<Integer, Long>();
        HashMap<Integer, Long> sumEachWorld = new HashMap<Integer, Long>();
        HashMap<Integer, Integer> totalMeasuresPerWorld = new HashMap<Integer, Integer>();
        for(int n_world = 1; n_world <= 4; n_world++) {
            sumEachWorld.put(n_world, 0L);
            totalMeasuresPerWorld.put(n_world, 0);
            previousMetric.put(n_world, 0L);
            nr_previous.put(n_world, 0);
        }

        for(int n_world = 1; n_world <= 4; n_world++) {
            ScanResult sr = getItemsForEndpoint(FoxRabbitObj.endpoint + String.valueOf(n_world));
            List<Map<String,AttributeValue>> listItems = sr.getItems();

            for(Map<String,AttributeValue> itemAttributes : listItems) {
                int wrld = Integer.parseInt(itemAttributes.get("world").getN());
                int previous_runs = Integer.parseInt(itemAttributes.get("nr_previous").getN());
                Long previous_metric = Long.parseLong(itemAttributes.get("statistic").getN());
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
            System.out.println(String.format("============= [FOXRABBIT - WORLD] ==============="));
            System.out.println(String.format("nr_previous = %d", nr_previous.get(n_world)));
            System.out.println(String.format("totalMeasuresPerWorld = %d", totalMeasuresPerWorld.get(n_world)));

            Integer numberMeasures = nr_previous.get(n_world)+totalMeasuresPerWorld.get(n_world);
            System.out.println(String.format("numberMeasures = %d", numberMeasures));

            int actualMeasures = numberMeasures;
            if (numberMeasures == 0) numberMeasures = 1;
            Long finalStat = (sumEachWorld.get(n_world) + previousMetric.get(n_world) * nr_previous.get(n_world)) / numberMeasures;
            System.out.println(String.format("sumEachWorld = %d", sumEachWorld.get(n_world)));
            System.out.println(String.format("previousMetric = %d", previousMetric.get(n_world)));
            System.out.println(String.format("nr_previous = %d", nr_previous.get(n_world)));
 
            System.out.println("[FOXRABBIT - WORLD "+ n_world + "]NEW STATISTIC "+ finalStat);
            client.putItem(FoxRabbitObj.generateRequest(tableName, actualMeasures, n_world, finalStat));
        }

        objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
    }

 // =====================================================================
 // ==========================                 ==========================
 // ==========================   GET METRICS   ==========================
 // ==========================                 ==========================
 // =====================================================================

    public static Map<Integer, Long> getFoxRabbitMetrics() {
        Map<Integer,Long> metricsPerWorld = new HashMap<Integer, Long>();
        
        ScanResult sr = getItemsForEndpoint(FoxRabbitObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(int n_world = 1; n_world <= 4; n_world++) {
            metricsPerWorld.put(n_world, 0L); 
        }
        for(Map<String,AttributeValue> itemAttributes : listItems) {
            int wrld = Integer.parseInt(itemAttributes.get("world").getN());
            Long previous_metric = Long.parseLong(itemAttributes.get("statistic").getN());
            metricsPerWorld.put(wrld, previous_metric);
        }

        return metricsPerWorld;
    }
    


    public static Map<String, List<Long>> getCompressMetrics() {
        Map<String,List<Long>> metricsPerFormat = new HashMap<String, List<Long>>();
        
        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();
            
        metricsPerFormat.put("bmp", new ArrayList<Long>(List.of(0L,0L)));
        metricsPerFormat.put("jpg", new ArrayList<Long>(List.of(0L,0L))); 
        metricsPerFormat.put("png", new ArrayList<Long>(List.of(0L,0L))); 

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            String format = itemAttributes.get("format").getS();
            Long previous_slope = Long.parseLong(itemAttributes.get("slope").getN());
            Long previous_origin = Long.parseLong(itemAttributes.get("origin").getN());
            metricsPerFormat.get(format).add(0, previous_slope);
            metricsPerFormat.get(format).add(1, previous_origin);
        }

        return metricsPerFormat;
    }

    public static List<Long> getInsectWarMetrics() {

        //Index 0 - PerRoundArmyEqual
        //Index 1 - PerArmyRound1
        List<Long> metrics = new ArrayList<Long>();

        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        //basic default value if there is no metric yet
        metrics.add(300000L);
        metrics.add(1L);

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            Long previous_perround = Long.parseLong(itemAttributes.get("roundincreasewhenarmyequal").getN());
            Long previous_perarmy = Long.parseLong(itemAttributes.get("round1perarmysize").getN());
            metrics.add(0, previous_perround);
            metrics.add(1, previous_perarmy);
        }

        return metrics;
    }





}
