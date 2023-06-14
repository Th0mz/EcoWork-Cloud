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

    private static Map<String, Map<String, Double>> metrics = new HashMap<String, Map<String, Double>>();

    private static Map<String, List<AbstractMetricObj>> objsToSave = new HashMap<String, List<AbstractMetricObj>>();

    private static List<CompressObj> bmpImages = new ArrayList<CompressObj>();
    private static List<CompressObj> pngImages = new ArrayList<CompressObj>();
    private static List<CompressObj> jpgImages = new ArrayList<CompressObj>();

    //first index for army size, second index for round number
    private static Map<Integer,  Map<Integer, InsectWarObj>> samearmysize = new HashMap<Integer, Map<Integer, InsectWarObj>>();
    private static List<InsectWarObj> roundOneEqualArmy = new ArrayList<InsectWarObj>();
    private static Double roundOneArmyOneInstr = 1000.0;
    

    public MetricsDB() {
        objsToSave.put(FoxRabbitObj.endpoint, new ArrayList<AbstractMetricObj>());
        objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
        objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
    }

    public static void main(String[] args) throws Exception {
        MetricsDB.createDB();
        //MetricsDB.saveMetric(new FoxRabbitObj(10, 3, 1, 1000));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 3, 1, 500));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 3, 1, 300));
        //MetricsDB.saveMetric(new FoxRabbitObj(10, 2, 1, 1500));
        //MetricsDB.saveMetric(new FoxRabbitObj(5, 2, 1, 750));
        //MetricsDB.saveMetric(new FoxRabbitObj(3, 2, 1, 450));
        //MetricsDB.saveMetric(new CompressObj("jpg", "0.2", 2, 4, 14));
        //MetricsDB.saveMetric(new CompressObj("jpg", "0.2", 3, 5, 16));
        //MetricsDB.saveMetric(new CompressObj("jpg", "0.2", 4, 6, 18));
        //MetricsDB.saveMetric(new CompressObj("jpg", "0.2", 5, 7, 20));
        //MetricsDB.saveMetric(new CompressObj("jpg", "0.2", 6, 8, 22));
        //MetricsDB.saveMetric(new InsectWarObj(1, 5, 5, 10000));
        //MetricsDB.saveMetric(new InsectWarObj(2, 5, 5, 1510000));

        //MetricsDB.saveMetric(new InsectWarObj(1, 3, 3, 6000));
        //MetricsDB.saveMetric(new InsectWarObj(2, 3, 3, 306000));

        //MetricsDB.saveMetric(new InsectWarObj(1, 2, 2, 4000));
        //MetricsDB.saveMetric(new InsectWarObj(4, 2, 2, 1804000));


        //updateAllMetrics();


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
        if(bmpImages.size() >= 3) updateBMP();
        if(pngImages.size() >= 3) updatePNG();
        if(jpgImages.size() >= 3) updateJPG();
        updateInsectWars();
        //DynamoUnlock()
    }




    public static void updateInsectWars() {
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
        List<InsectWarObj> roundOneBaselines = new ArrayList<InsectWarObj>();
        for (Integer armySize : samearmysize.keySet()) {
            if (samearmysize.get(armySize).size() > 1) {
                for (InsectWarObj obj1 : samearmysize.get(armySize).values()) {
                    if (obj1.getMax() != 1) continue;
                    roundOneBaselines.add(obj1);
                    for (InsectWarObj obj2 : samearmysize.get(armySize).values()) {
                        if (obj1.getMax() == obj2.getMax()) continue;
                        nr_measures++;
                        int roundDif = obj2.getMax() - obj1.getMax();
                        Long instrDif = obj2.getInstructions() - obj1.getInstructions();
                        Double functionOfRound = instrDif*1.0 / (armySize*roundDif*1.0);
                        estimatePerRound += functionOfRound;
                    }
                }
            }
        }
        //first index for army size, second index for round number 
        samearmysize = new HashMap<Integer, Map<Integer, InsectWarObj>>();
        for(InsectWarObj obj : roundOneBaselines) {
            if(!samearmysize.containsKey(obj.getArmy1()))
                samearmysize.put(obj.getArmy1(), new HashMap<Integer, InsectWarObj>());
            samearmysize.get(obj.getArmy1()).put(obj.getMax(), obj);
        }


        //Calculus of per army size variation as a function of round,
        // when armies are equal and round is one
        int nr_measures_roundone = 0;
        Double estimatePerArmy = 0.0;
        for (InsectWarObj obj1 : roundOneEqualArmy) {
            System.out.println("----------->>>>>-----------");
            System.out.println("OBJ1 Army: " + obj1.getArmy1());

            Double armyRatio =  obj1.getArmy1()*1.0/1.0; //army1 is the baseline for comparison
            Double instrRatio = obj1.getInstructions()*1.0/roundOneArmyOneInstr;
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
    
        System.out.println(String.format("[INSECTWAR] NEW STATISTIC: PERROUND-%f PERARMY-%f", finalPerRound, finalPerArmy));
        client.putItem(InsectWarObj.generateRequest(tableName, nr_finalPerArmy, finalPerArmy, 
                nr_finalPerRound, finalPerRound));

        objsToSave.put(InsectWarObj.endpoint, new ArrayList<AbstractMetricObj>());
        roundOneEqualArmy.clear();
    }




    public static void updateBMP() {
        if(!objsToSave.containsKey(CompressObj.endpoint)) {
            objsToSave.put(CompressObj.endpoint, new ArrayList<AbstractMetricObj>());
            return;
        }

        HashMap<String, Integer> nr_previous = new HashMap<String, Integer>();
        HashMap<String, Double> previousSlope = new HashMap<String, Double>();
        HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

        HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
        HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
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

        for(CompressObj obj : bmpImages) {
            x.add(obj.getHeight().doubleValue());
            y.add(obj.getInstructions().doubleValue());
        }

        LinearRegression regression = new LinearRegression(x, y);

        Double newSlope = regression.slope();
        Double newOrigin = regression.intercept();
        System.out.println("NEW SLOPE: " + newSlope);
        System.out.println("NEW ORIGIN: " + newOrigin);

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
        HashMap<String, Double> previousSlope = new HashMap<String, Double>();
        HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

        HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
        HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
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
        HashMap<String, Double> previousSlope = new HashMap<String, Double>();
        HashMap<String, Double> previousOrigin = new HashMap<String, Double>();

        HashMap<String, Double> sumEachSlope = new HashMap<String, Double>();
        HashMap<String, Double> sumEachOrigin = new HashMap<String, Double>();
        List<Integer> totalMeasuresPerWorld = new ArrayList<Integer>();

        List<String> formats = Arrays.asList("bmp", "png", "jpg"); //TODO: check formats
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
    }

 // =====================================================================
 // ==========================                 ==========================
 // ==========================   GET METRICS   ==========================
 // ==========================                 ==========================
 // =====================================================================

    public static Map<Integer, Double> getFoxRabbitMetrics() {
        Map<Integer,Double> metricsPerWorld = new HashMap<Integer, Double>();
        
        ScanResult sr = getItemsForEndpoint(FoxRabbitObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        for(int n_world = 1; n_world <= 4; n_world++) {
            metricsPerWorld.put(n_world, 0.0); 
        }
        for(Map<String,AttributeValue> itemAttributes : listItems) {
            int wrld = Integer.parseInt(itemAttributes.get("world").getN());
            Double previous_metric = Double.parseDouble(itemAttributes.get("statistic").getN());
            metricsPerWorld.put(wrld, previous_metric);
        }

        return metricsPerWorld;
    }
    


    public static Map<String, List<Double>> getCompressMetrics() {
        Map<String,List<Double>> metricsPerFormat = new HashMap<String, List<Double>>();
        
        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();
            
        metricsPerFormat.put("bmp", new ArrayList<Double>(List.of(0.0,0.0)));
        metricsPerFormat.put("jpg", new ArrayList<Double>(List.of(0.0,0.0))); 
        metricsPerFormat.put("png", new ArrayList<Double>(List.of(0.0,0.0))); 

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            String format = itemAttributes.get("format").getS();
            Double previous_slope = Double.parseDouble(itemAttributes.get("slope").getN());
            Double previous_origin = Double.parseDouble(itemAttributes.get("origin").getN());
            metricsPerFormat.get(format).add(0, previous_slope);
            metricsPerFormat.get(format).add(1, previous_origin);
        }

        return metricsPerFormat;
    }

    public static List<Double> getInsectWarMetrics() {

        //Index 0 - PerRoundArmyEqual
        //Index 1 - PerArmyRound1
        List<Double> metrics = new ArrayList<Double>();

        ScanResult sr = getItemsForEndpoint(CompressObj.endpoint);
        List<Map<String,AttributeValue>> listItems = sr.getItems();

        //basic default value if there is no metric yet
        metrics.add(300000.0);
        metrics.add(1.0);

        for(Map<String,AttributeValue> itemAttributes : listItems) {
            Double previous_perround = Double.parseDouble(itemAttributes.get("roundincreasewhenarmyequal").getN());
            Double previous_perarmy = Double.parseDouble(itemAttributes.get("round1perarmysize").getN());
            metrics.add(0, previous_perround);
            metrics.add(1, previous_perarmy);
        }

        return metrics;
    }





}
