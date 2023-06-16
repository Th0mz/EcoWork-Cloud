package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.requests.CompressRequest;
import pt.ulisboa.tecnico.cnv.requests.Request;
import pt.ulisboa.tecnico.cnv.util.InstanceState;
import pt.ulisboa.tecnico.cnv.util.SystemState;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
//import com.amazonaws.core.SdkBytes;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.auth.AWSCredentialsProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.ArrayList;
import java.util.TimerTask;


public class LBCompressImageHandler implements HttpHandler {

    private SystemState state;
    private final String path = "/compressimage";
    private Timer timer = new Timer();

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

        String whereToExecute = chooseProcessor(request);
        if (whereToExecute.equals("Worker")) {

            System.out.println("sending request");
            sendRequest(request);

        } else if (whereToExecute.equals("Lambda")) {
            String functionName = "compression-lambda";
            String jsonArgs = String.format("{\"body\":\"%s\",\"compressionFactor\":\"%f\",\"targetFormat\":\"%s\"}",
                    resultSplits[1], Double.parseDouble(compressionFactor), targetFormat);

            AWSCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
            AWSLambda awsLambda = AWSLambdaClientBuilder.standard().withCredentials(credentialsProvider).build();
            String response = invokeLambda(awsLambda, functionName, jsonArgs, request);
            awsLambda.shutdown();

            if(response != null) {
                //send response back
                byte[] output = String.format("data:image/%s;base64,%s", targetFormat, response).getBytes();
                exchange.sendResponseHeaders(200, output.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(output);
                outputStream.close();

            }

        } else  if (whereToExecute.equals("Wait")) {
            System.out.println("[LB]: Request too big while instances launching, waiting ...");
            timer.schedule(new WaitForWebserverTask(request, state.getPending()), 5000); 

        }
       
    }

    private String chooseProcessor(Request request){
        double lastKnownAvg = this.state.getCPUAvg();
        if(lastKnownAvg > 80 ) {
            if (this.state. getPendingNr() > 0 && request.getCost() < 5000000){
                //high cpu but small request -> lambda
                return "Lambda";
            } else if (this.state. getPendingNr() > 0 && request.getCost() > 10000000){
                //high cpu but big request -> wait
                return "Wait";
            } else {
                //high cpu average request -> assign to worker
                return "Worker";
            }
        }
        //normal case -> assign to worker
        return "Worker";
    }

    private class WaitForWebserverTask extends TimerTask {
        private CompressRequest request;
        private ArrayList<String> pending;

        public WaitForWebserverTask(CompressRequest request, ArrayList<String> pend) {
            this.request = request;
            this.pending = pend;
        }

        @Override
        public void run() {
            boolean canRun = false;
            
            for (String i: pending){
                if (state.isRunning(i)) { 
                    canRun = true;
                    break;
                }
            }

            if(canRun) { 
                System.out.println("[LB]: Waiting request now sent...");
                sendRequest(request); 
            } 
            else {
                System.out.println("[LB]: Waiting request still waiting...");
                WaitForWebserverTask task = new WaitForWebserverTask(this.request, this.pending);
                timer.schedule(task, 5000);
            }
        }
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

                String re = new String(responseBody);
                //System.out.println(re);

                // Close connections
                responseStream.close();
                connection.disconnect();
                return;
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


    public String invokeLambda(AWSLambda awsLambda, String functionName, String json, CompressRequest workerRequest) {
        String response = null;
        try {
            //SdkBytes payload = SdkBytes.fromUtf8String(json) ;

            InvokeRequest request = new InvokeRequest().withFunctionName(functionName).withPayload(json);
            System.out.println("[LB]: Invoking Lambda function " + functionName);
            InvokeResult res = awsLambda.invoke(request);
            if(res.getStatusCode() == 200) {
                byte[] responseAux = res.getPayload().array();
                String re = new String(responseAux, 1, responseAux.length - 2);
                //System.out.println(re);
                return re;
            }
        } catch(AWSLambdaException e) {
            System.err.println(e.getMessage());
            System.out.println("[LB]: Lambda execution failed, retry on worker");
            sendRequest(workerRequest);
        }
        return response;
    }
    
}
