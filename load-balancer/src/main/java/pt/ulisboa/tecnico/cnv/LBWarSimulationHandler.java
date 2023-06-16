package pt.ulisboa.tecnico.cnv;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.requests.InsectWarsRequest;
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
import java.util.Timer;
import java.util.ArrayList;
import java.util.TimerTask;


public class LBWarSimulationHandler implements HttpHandler {

    private SystemState state;
    private final String path = "/insectwar";
    private Timer timer = new Timer();
    

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

        System.out.println("Calculating cost...");
        Long cost = calculateCost(max, army1, army2).longValue();
        System.out.println("cost = " + cost);

        InsectWarsRequest request = new InsectWarsRequest(parameters, cost, exchange);
        String whereToExecute = chooseProcessor(request);
        if (whereToExecute.equals("Worker")) {

            System.out.println("sending request");
            sendRequest(request);

        } else if (whereToExecute.equals("Lambda")) {

            String functionName = "insectWar-lambda";
            String jsonArgs = String.format("{\"max\":\"%d\",\"army1\":\"%d\",\"army2\":\"%d\"}",
                    max, army1, army2);

            AWSCredentialsProvider credentialsProvider = new EnvironmentVariableCredentialsProvider();
            AWSLambda awsLambda = AWSLambdaClientBuilder.standard().withCredentials(credentialsProvider).build();
            byte[] response = invokeLambda(awsLambda, functionName, jsonArgs, request);
            awsLambda.shutdown();
            if(response != null){
                //send response back
                exchange.sendResponseHeaders(200, response.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(response);
                outputStream.close();

            }

        } else  if (whereToExecute.equals("Wait")) {
            System.out.println("[LB]: Request too big while instances launching, waiting ...");
            timer.schedule(new WaitForWebserverTask(request, state.getPending()), 5000); 
        }
        

    }

    private class WaitForWebserverTask extends TimerTask {
        private InsectWarsRequest request;
        private ArrayList<String> pending;

        public WaitForWebserverTask(InsectWarsRequest request, ArrayList<String> pend) {
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

    public Double calculateCost(long round, long army1, long army2) {
        Double value = 900502.0; //(which is (1,1,1)) tested locally
        ArrayList<Double> metrics = state.getInsectWarMetrics(); //index0- perRound; index1-perArmyRound1
        for (Double d: metrics){
            System.out.print(d);
            System.out.print("||");
        }
        System.out.println("\n");
        if (army1 == army2) {
            value = value * (metrics.get(1)*army2);
            value = value + metrics.get(0) * army2 * (round-1);
            return value;
        } else if (army2 < army1) {
            value = value * (metrics.get(1)*army2);
            value = value + metrics.get(0) * army2 * (round-1);
            int index = (int) (((army1*1.0/army2) - 1) / 0.1) - 1;
            if(index > 88) index = 88; //there are only 89 ratios stored, after that the change is irrelevant
            value = value * state.getPerArmyRatio().get(index) * (army1/army2);
            return value;
        } 
        else { //army1 < army2
            value = value * (metrics.get(1)*army1);
            value = value + metrics.get(0) * army1 * (round-1);
            int index = (int) (((army2*1.0/army1) - 1) / 0.1) - 1;
            if(index > 88) index = 88; //there are only 89 ratios stored, after that the change is irrelevant
            value = value * state.getPerArmyRatio().get(index) * (army2/army1);
            return value;
        }
    }

    public void sendRequest(InsectWarsRequest request) {
        Map<String, String> parameters = request.getParameters();
        HttpExchange exchange = request.getClient();
        InstanceState bestInstance = null;

        while (request.getTries() < 3) {
            try {

                // get best instance (one with the lowest instructions)
                bestInstance = state.getInstance();
                bestInstance.newRequest(request);

                // Create a connection to the target URL
                URL url = new URL(buildRequestURL(bestInstance.getUrl(), parameters));
                System.out.println("[LB-war] Sending request to " + url + "for the " + request.getTries() + "x");
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
                byte[] requestBody = exchange.getRequestBody().readAllBytes();
                if (requestBody.length > 0) {
                    connection.getOutputStream().write(requestBody);
                }


                /*  Process received response from webserver and send it to the client
                 *  ================================================================== */
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    continue;
                }

                boolean finished = bestInstance.finishRequest(request.getId());
                if (!finished) {
                    System.out.println("[LB-war] Instance failed to deliver result (status code != 200), going to try again...");
                    bestInstance.finishRequest(request.getId());
                    continue;
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

                // successfully handled request
                return;
            } catch (IOException e) {
                System.out.println("[LB-war] Instance failed to deliver result (exception thrown), going to try again...");
                if (bestInstance != null) {
                    bestInstance.finishRequest(request.getId());
                }
            }
        }

        System.out.println("[LB-war] Tried to send request three times but none when through");
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

    public byte[] invokeLambda(AWSLambda awsLambda, String functionName, String json, InsectWarsRequest workerRequest) {
        byte[] response = null;
        try {
            //SdkBytes payload = SdkBytes.fromUtf8String(json) ;

            InvokeRequest request = new InvokeRequest().withFunctionName(functionName).withPayload(json);
            System.out.println("[LB]: Invoking Lambda function " + functionName);
            InvokeResult res = awsLambda.invoke(request);
            if(res.getStatusCode() == 200) {
                response = res.getPayload().array();
                String re = new String(response, 1, response.length - 2).replace("\\","");
                return re.getBytes() ;
            } else {
                res.getPayload().array();
            }
        } catch(AWSLambdaException e) {
            System.err.println(e.getMessage());
            System.out.println("[LB]: Lambda execution failed, retry on worker");
            sendRequest(workerRequest);
        }
        return response;
    }
}

