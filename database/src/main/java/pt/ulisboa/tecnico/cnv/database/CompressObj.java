package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class CompressObj extends AbstractMetricObj {
    private String format;
    private String factor;

    public CompressObj(String formatArg, String factorArg) {
        format = formatArg;
        factor = factorArg;
    }

    public String getFormat() {return format;}
    public String getFactor() {return factor;}

    public static PutItemRequest generateRequest(String tableName, String format, String factor, int nr_previous) {
        //TODO: What is actually the metric
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("compress"));
        itemValues.put("format", new AttributeValue(format));
        itemValues.put("factor", new AttributeValue(factor));
        return new PutItemRequest(tableName, itemValues);
    }
}
