package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class FoxRabbitObj extends AbstractMetricObj {
    private int n_generations;
    private int world;
    private int scenario;
    private int instructions;

    public FoxRabbitObj(int gen, int wrld, int scen, int instr) {
        n_generations = gen;
        world = wrld;
        scenario = scen;
        instructions = instr;
    }

    public int getGen(){return n_generations;}
    public int getWorld(){return world;}
    public int getScenario(){return scenario;}
    public int getInstructions(){return instructions;}

    public static PutItemRequest generateRequest(String tableName, int nr_previous, int world, Long stat) {
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("foxrabbit"));
        itemValues.put("world", new AttributeValue().withN(Integer.toString(world)));
        //TODO: itemValues.put("average", new AttributeValue().withN(Integer.toString(scenario)));
        itemValues.put("nr_previous", new AttributeValue().withN(Integer.toString(nr_previous)));
        itemValues.put("statistic", new AttributeValue().withN(Long.toString(stat)));
        return new PutItemRequest(tableName, itemValues);
    }

    public int getWeight() {
        return instructions/n_generations;
    }

}
