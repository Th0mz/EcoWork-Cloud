package pt.ulisboa.tecnico.cnv;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.database.MetricsDB;
import pt.ulisboa.tecnico.cnv.util.SystemState;

public class LoadBalancer {
    private static final int port = 8000;

    public static void main(String[] args) throws Exception {
        SystemState state = new SystemState();
        AutoScaler autoScaler = new AutoScaler(state);
        MetricsDB.initialize();
        autoScaler.start();

        System.out.println("Running load balancer on port " + port + "...");
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/simulate", new LBSimulationHandler(state));
        server.createContext("/compressimage", new LBCompressImageHandler(state));
        server.createContext("/insectwar", new LBWarSimulationHandler(state));
        server.createContext("/test", new RootHandler());


        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

}