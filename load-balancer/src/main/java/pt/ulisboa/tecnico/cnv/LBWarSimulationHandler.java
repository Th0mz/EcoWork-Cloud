package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.requests.InsectWarsRequest;
import pt.ulisboa.tecnico.cnv.util.SystemState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;


public class LBWarSimulationHandler implements HttpHandler {

    private SystemState state;
    private final String path = "/insectwar";


    public LBWarSimulationHandler(SystemState state) {
        this.state = state;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Handling CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }


        /*  Parse received request
         *  ====================== */
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        Map<String, String> parameters = queryToMap(query);

        long max = Long.parseLong(parameters.get("max"));
        long army1 = Long.parseLong(parameters.get("army1"));
        long army2 = Long.parseLong(parameters.get("army2"));

        Long cost = calculateCost(max, army1, army2).longValue();
        InsectWarsRequest request = new InsectWarsRequest(parameters, cost, exchange);
        sendRequest(request);

    }

    public Double calculateCost(long round, long army1, long army2) {
        Double value = 900502.0; //(which is (1,1,1)) tested locally
        ArrayList<Double> metrics = state.getInsectWarMetrics(); //index0- perRound; index1-perArmyRound1
        if (army2 <= army1) {
            value = value * (metrics.get(1)*army2);
            value = value + metrics.get(0) * army2 * round;
            int index = (int) (((army1*1.0/army2) - 1) / 0.1);
            if(index > 89) index = 89; //there are only 89 ratios stored, after that the change is irrelevant
            value = value * state.getPerArmyRatio().get(index) * (army1/army2);
            return value;
        } 
        else { //army1 < army2
            value = value * (metrics.get(1)*army1);
            value = value + metrics.get(0) * army1 * round;
            int index = (int) (((army2*1.0/army1) - 1) / 0.1);
            if(index > 89) index = 89; //there are only 89 ratios stored, after that the change is irrelevant
            value = value * state.getPerArmyRatio().get(index) * (army2/army1);
            return value;
        } 

        //value = value * (perArmySize * smallerArmy)
        //value = value + perRound * smallerArmy * round;
        //value = value * perArmyRatio.get(army1/army2 * ) * (armyRatio); //perArmyRatio is an array with the ratios for the 100 0.1ratio steps
        //return value;
        //return 1000L;
    }

    public void sendRequest(InsectWarsRequest request) {
        try {

            Map<String, String> parameters = request.getParameters();
            HttpExchange exchange = request.getClient();


            // TODO : choose instance
            // TODO : add this request to its request list
            // Create a connection to the target URL
            URL url = new URL(buildRequestURL(state.getInstance(), parameters));
            System.out.println("[LB-war] Sending request to " + url);

            /* Create connection to webserver
             *  ============================== */
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method and headers from the client's request
            connection.setRequestMethod(exchange.getRequestMethod());
            connection.setDoOutput(true);
            exchange.getRequestHeaders().forEach((key, value) -> connection.setRequestProperty(key, value.get(0)));

            // Forward the client's request body, if present
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            if (requestBody.length > 0) {
                connection.getOutputStream().write(requestBody);
            }


            /*  Process received response from webserver and send it to the client
             *  ================================================================== */
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                // TODO : instance failed must send to other instance
            }

            InputStream responseStream = connection.getInputStream();
            byte[] responseBody = responseStream.readAllBytes();

            // Send the target server's response body back to the client
            exchange.sendResponseHeaders(responseCode, responseBody.length);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseBody);
            outputStream.close();

            // Close connections
            responseStream.close();
            connection.disconnect();
        } catch (IOException e) {
            // TODO
        }
    }


    public Map<String, String> queryToMap(String query) {
        if(query == null) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        for(String param : query.split("&")) {
            String[] entry = param.split("=");
            if(entry.length > 1) {
                result.put(entry[0], entry[1]);
            }else{
                result.put(entry[0], "");
            }
        }
        return result;
    }

    private String buildRequestURL(String instanceURL, Map<String, String> urlParams) {

        String requestURL = instanceURL + path;
        int numArgs = 0;
        for (String key : urlParams.keySet()) {
            if (numArgs == 0) {
                requestURL = requestURL + "?";
            }

            numArgs++;
            requestURL = requestURL + key + "=" + urlParams.get(key);
            if (numArgs < urlParams.size()) {
                requestURL = requestURL + "&";
            }

        }

        return requestURL;
    }
}

