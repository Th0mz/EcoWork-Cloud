package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class InsectWarObj extends AbstractMetricObj {
    public static String endpoint = "war";
    private int max;
    private int army1;
    private int army2;
    private long instructions;

    public InsectWarObj(int m, int a1, int a2, long instr) {
        max = m;
        army1 = a1;
        army2 = a2;
        instructions = instr;
    }

    public int getMax(){return max;}
    public int getArmy1(){return army1;}
    public int getArmy2(){return army2;}
    public long getInstructions(){return instructions;}

    public static PutItemRequest generateRequest(String tableName, Integer nr_round1perarmysize, Double round1perarmysize, 
            Integer nr_roundincreasewhenarmyequal, Double roundincreasewhenarmyequal) {
        //TODO: What is actually the metric
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("war"));
        itemValues.put("round1perarmysize", new AttributeValue().withN(Double.toString(round1perarmysize)));
        itemValues.put("nr_round1perarmysize", new AttributeValue().withN(Integer.toString(nr_round1perarmysize)));


        itemValues.put("nr_roundincreasewhenarmyequal", new AttributeValue().withN(Integer.toString(nr_roundincreasewhenarmyequal)));
        itemValues.put("roundincreasewhenarmyequal", new AttributeValue().withN(Double.toString(roundincreasewhenarmyequal)));
        
        //TODO: MAYBE? itemValues.put("instrarmyratio", new AttributeValue().withN(Double.toString(instrarmyratio)));
        //itemValues.put("nr_previous", new AttributeValue().withN(Long.toString(nr_previous)));
        return new PutItemRequest(tableName, itemValues);
    }

    public static PutItemRequest generateRatioRequest(String tableName, int index, Double perArmyRatio, int count) {
        //TODO: What is actually the metric
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("war"+String.valueOf(index)));
        itemValues.put("perArmyRatio", new AttributeValue().withN(Double.toString(perArmyRatio)));
        itemValues.put("nr_previous", new AttributeValue().withN(Integer.toString(count)));

        //TODO: MAYBE? itemValues.put("instrarmyratio", new AttributeValue().withN(Double.toString(instrarmyratio)));
        //itemValues.put("nr_previous", new AttributeValue().withN(Long.toString(nr_previous)));
        return new PutItemRequest(tableName, itemValues);
    }
}
