package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.requests.FoxAndRabbitsRequest;
import pt.ulisboa.tecnico.cnv.requests.InsectWarsRequest;
import pt.ulisboa.tecnico.cnv.util.SystemState;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LBSimulationHandler implements HttpHandler {

    private SystemState state;
    private final String path = "/simulate";

    public LBSimulationHandler(SystemState state) {
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

        int n_generations = Integer.parseInt(parameters.get("generations"));
        int world = Integer.parseInt(parameters.get("world"));
        int n_scenario = Integer.parseInt(parameters.get("scenario"));

        // TODO : estimate cost
        Long cost = calculateCost(world, n_generations).longValue();
        FoxAndRabbitsRequest request = new FoxAndRabbitsRequest(parameters, cost, exchange);
        sendRequest(request);
    }

    public Double calculateCost(int world, int gen) {
        //return worldWeights.get(world) * gen; //worldWeights contains instr/gener for each world
        //return 1000L;
        return state.getFoxRabbitMetrics().get(world) * gen;
    }

    public void sendRequest(FoxAndRabbitsRequest request) {
        try {
            Map<String, String> parameters = request.getParameters();
            HttpExchange exchange = request.getClient();

            // TODO : choose instance
            // TODO : add this request to its request list

            // Create a connection to the target URL
            URL url = new URL(buildRequestURL(state.getInstance(), parameters));
            System.out.println("[LB-simulation] Sending request to " + url);

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

            // DEBUG : System.out.println("RESPONSE");
            // DEBUG : System.out.println(new String(responseBody, StandardCharsets.UTF_8));


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
            // TODO : instance failed must send to other instance
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
