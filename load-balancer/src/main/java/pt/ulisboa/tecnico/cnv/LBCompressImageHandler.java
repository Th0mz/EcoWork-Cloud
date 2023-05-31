package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.util.SystemState;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class LBCompressImageHandler implements HttpHandler {

    private SystemState state;
    private final String path = "/compressimage";

    public LBCompressImageHandler(SystemState state) {
        this.state = state;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Handling CORS
        if (exchange.getRequestHeaders().getFirst("Origin") != null) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", exchange.getRequestHeaders().getFirst("Origin"));
        }

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            System.out.println("[LB-compress] Ignore case");

            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,API-Key");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        /*  Parse received request
         *  ====================== */

        /*
         *  ====================== */
        // TODO : choose to which instance is going to handle the

        // Create a connection to the target URL
        URL url = new URL(state.getInstance() + path);
        System.out.println("[LB-compress] Sending request to " + url);

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
        System.out.println("Response code = " + responseCode);

        InputStream responseStream = connection.getInputStream();
        byte[] responseBody = responseStream.readAllBytes();

        System.out.println("RESPONSE");
        // DEBUG : System.out.println(new String(responseBody, StandardCharsets.UTF_8));


        // Send the target server's response body back to the client
        exchange.sendResponseHeaders(responseCode, responseBody.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBody);
        outputStream.close();

        // Close connections
        responseStream.close();
        connection.disconnect();
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
