package pt.ulisboa.tecnico.cnv.webserver;

import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.URI;

public class MetricsHandler implements HttpHandler {

    TestCPU metrics;
    public MetricsHandler(){
        metrics = new TestCPU();
        metrics.start();
        
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Handling CORS
        he.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (he.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            System.out.print("\n\nRESPONSE NOT SENT\n\n" );
            he.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            he.sendResponseHeaders(204, -1);
            return;
        }

        // parse request
        String k = String.format("%.2f", metrics.getAvg());
        he.sendResponseHeaders(200, k.length());
        OutputStream os = he.getResponseBody();
        os.write(k.getBytes());


        os.close();
    }
    
}
