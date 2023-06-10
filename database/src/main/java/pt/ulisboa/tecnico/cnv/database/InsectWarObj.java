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

    public static PutItemRequest generateRequest(String tableName, int max, int armydiff, int nr_previous) {
        //TODO: What is actually the metric
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("war"));
        itemValues.put("max", new AttributeValue().withN(Integer.toString(max)));
        itemValues.put("armydiff", new AttributeValue().withN(Integer.toString(armydiff)));
        return new PutItemRequest(tableName, itemValues);
    }
}
