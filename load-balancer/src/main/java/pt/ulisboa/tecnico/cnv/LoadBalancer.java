package pt.ulisboa.tecnico.cnv;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;


public class LoadBalancer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        //server.createContext("/", new RootHandler());
        server.createContext("/simulate", new SimulationHandler());
        server.createContext("/compressimage", new CompressImageHandlerImpl());
        server.createContext("/insectwar", new WarSimulationHandler());
        server.start();

    }

}