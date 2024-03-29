package pt.ulisboa.tecnico.cnv.insectwar;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpHandler;

import javassist.bytecode.annotation.ArrayMemberValue;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URI;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import pt.ulisboa.tecnico.cnv.javassist.tools.ServerICount;
import pt.ulisboa.tecnico.cnv.database.InsectWarObj;
import pt.ulisboa.tecnico.cnv.database.MetricsDB;

public class WarSimulationHandler implements HttpHandler, RequestHandler<Map<String, String>, String> {

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        // parse request
        URI requestedUri = he.getRequestURI();
        String query = requestedUri.getRawQuery();
        Map<String, String> parameters = queryToMap(query);

        int max = Integer.parseInt(parameters.get("max"));
        int army1 = Integer.parseInt(parameters.get("army1"));
        int army2 = Integer.parseInt(parameters.get("army2"));

        InsectWars insect_wars = new InsectWars();
        String response = insect_wars.war(max, army1, army2);
        long instructions = ServerICount.getInstructions(Thread.currentThread().getId());
        System.out.println(String.format("NUMBER OF INSTRUCITONS OF THREAD %s is %d", Thread.currentThread().getId(), instructions));
        String argsRequest = Integer.toString(max) + ":" + Integer.toString(army1) + ":" + Integer.toString(army2);
        MetricsDB.saveMetric(new InsectWarObj(max, army1, army2, instructions));

        he.sendResponseHeaders(200, response.toString().length());
        OutputStream os = he.getResponseBody();
        os.write(response.getBytes());

        os.close();
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

    private String handleRequest(int max, int army1, int army2) {
        InsectWars insect_wars = new InsectWars();
        String response = insect_wars.war(max, army1, army2);

        return response;
    }

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        return handleRequest(Integer.parseInt(event.get("max")), Integer.parseInt(event.get("army1")), Integer.parseInt(event.get("army2")));
    }
}