package pt.ulisboa.tecnico.cnv;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import pt.ulisboa.tecnico.cnv.util.SystemState;

public class LoadBalancer {
    private static final int port = 80;

    public static void main(String[] args) throws Exception {
        SystemState state = new SystemState();

        System.out.println("Running load balancer on port " + port + "...");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/simulate", new LBSimulationHandler(state));
        server.createContext("/compressimage", new LBCompressImageHandler(state));
        server.createContext("/insectwar", new LBWarSimulationHandler(state));

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

}