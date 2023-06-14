package pt.ulisboa.tecnico.cnv.requests;

import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;
import java.util.Map;

public class FoxAndRabbitsRequest extends Request {

    private Map<String, String> parameters;

    public FoxAndRabbitsRequest(Map<String, String> parameters, long cost, HttpExchange client) {
        super(cost, client);
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }
}
