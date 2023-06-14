package pt.ulisboa.tecnico.cnv.database;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

public class CompressObj extends AbstractMetricObj {
    public static String endpoint = "compress";
    private String format;
    private String factor;
    private Integer height;
    private Integer pixels;
    private Long instructions;

    public CompressObj(String formatArg, String factorArg, int height_, int pixels_, long instr) {
        format = formatArg;
        factor = factorArg;
        height = height_;
        pixels = pixels_;
        instructions = instr;
    }

    public String getFormat() {return format;}
    public String getFactor() {return factor;}
    public Integer getHeight() {return height;}
    public Integer getPixels() {return pixels;}
    public Long getInstructions() {return instructions;}

    public static PutItemRequest generateRequest(String tableName, String format, double slope, double origin, int nr_previous) {
        //TODO: What is actually the metric
        Map<String, AttributeValue> itemValues = new HashMap<String, AttributeValue>();
        itemValues.put("endpoint", new AttributeValue("compress"+format));
        itemValues.put("format", new AttributeValue(format));
        itemValues.put("slope", new AttributeValue().withN(Double.toString(slope)));
        itemValues.put("origin", new AttributeValue().withN(Double.toString(origin)));
        itemValues.put("nr_previous", new AttributeValue().withN(Integer.toString(nr_previous)));
        return new PutItemRequest(tableName, itemValues);
    }
}
