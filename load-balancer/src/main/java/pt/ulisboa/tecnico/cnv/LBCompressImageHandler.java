package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.requests.CompressRequest;
import pt.ulisboa.tecnico.cnv.util.InstanceState;
import pt.ulisboa.tecnico.cnv.util.SystemState;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
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
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        String result = new String(requestBody);
        String[] resultSplits = result.split(",");
        String targetFormat = resultSplits[0].split(":")[1].split(";")[0];
        String compressionFactor = resultSplits[0].split(":")[2].split(";")[0];


        byte[] decoded = Base64.getDecoder().decode(resultSplits[1]);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
        BufferedImage bi = ImageIO.read(bais);

        int height = bi.getHeight();

        Long cost = calculateCost(targetFormat, height, height*height).longValue();
        CompressRequest request = new CompressRequest(requestBody, cost, exchange);
        sendRequest(request);
    }

    public Double calculateCost(String format, int height, int pixels) {
        double slope = state.getCompressionMetrics().get(format).get(0);
        double origin = state.getCompressionMetrics().get(format).get(1);
        if(format.equals("bmp")) return slope*height + origin;
        return slope*pixels+origin;
    }

    public void sendRequest(CompressRequest request) {
        byte[] requestBody = request.getResponseBody();
        HttpExchange exchange = request.getClient();
        InstanceState bestInstance = null;

        while (request.getTries() < 3) {
            try {

                // get best instance (one with the lowest instructions)
                bestInstance = state.getInstance();
                bestInstance.newRequest(request);

                // Create a connection to the target URL
                URL url = new URL(bestInstance.getUrl() + path);
                System.out.println("[LB-compress] Sending request to " + url + "for the " + request.getTries() + "x");
                System.out.println(" > Request cost : " + request.getCost());
                System.out.println(" > Current Instance Workload : " + bestInstance.getExecutingInstructions());


                /* Create connection to webserver
                 *  ============================== */
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set request method and headers from the client's request
                connection.setRequestMethod(exchange.getRequestMethod());
                connection.setDoOutput(true);
                exchange.getRequestHeaders().forEach((key, value) -> connection.setRequestProperty(key, value.get(0)));

                // Forward the client's request body, if present
                if (requestBody.length > 0) {
                    connection.getOutputStream().write(requestBody);
                }

                /*  Process received response from webserver and send it to the client
                 *  ================================================================== */
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    System.out.println("[LB-compress] Instance failed to deliver result (status code != 200), going to try again...");
                    bestInstance.finishRequest(request.getId());
                    continue;
                }

                boolean finished = bestInstance.finishRequest(request.getId());
                if (!finished) {
                    System.out.println("Error : Request " + request.getId() + "tryed to finish at instance " + bestInstance.getId() + "but wasn't found");
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
                System.out.println("[LB-compress] Instance failed to deliver result (exception thrown), going to try again...");
                if (bestInstance != null) {
                    bestInstance.finishRequest(request.getId());
                }
            }
        }

        System.out.println("[LB-compress] Tried to send request three times but none when through");
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
