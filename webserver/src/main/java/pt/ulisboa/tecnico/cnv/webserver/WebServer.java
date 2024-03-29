package pt.ulisboa.tecnico.cnv.webserver;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.foxrabbit.SimulationHandler;
import pt.ulisboa.tecnico.cnv.compression.CompressImageHandlerImpl;
import pt.ulisboa.tecnico.cnv.database.MetricsDB;
import pt.ulisboa.tecnico.cnv.insectwar.WarSimulationHandler;

public class WebServer {
    public static void main(String[] args) throws Exception {
        MetricsDB.createDB();
        //MetricsDB.insertNewItem();
        //MetricsDB.getAllItems();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        //server.createContext("/", new RootHandler());
        server.createContext("/simulate", new SimulationHandler());
        server.createContext("/compressimage", new CompressImageHandlerImpl());
        server.createContext("/insectwar", new WarSimulationHandler());
        server.createContext("/test", new RootHandler());
        server.createContext("/cpu", new MetricsHandler());
        
        server.start();

        
        class UploadMetricsTask extends TimerTask {
            @Override
            public void run() {
                System.out.println("Running scheduled fixed task");
                MetricsDB.updateAllMetrics();
              
            }
        }

        new Timer().scheduleAtFixedRate(new UploadMetricsTask(), 0, 20000);
        

    }
}
