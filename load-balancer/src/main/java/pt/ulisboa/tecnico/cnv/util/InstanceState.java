package pt.ulisboa.tecnico.cnv.util;

import pt.ulisboa.tecnico.cnv.requests.Request;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

public class InstanceState {

    private String id;
    private String url;
    private Double cpuAvg = 25.0;
    private long executingInstructions;
    private ConcurrentHashMap<String, Request> requests;
    private LocalDateTime startingTime;
    private boolean pending = true;


    public InstanceState(String id, String url) {
        this.id = id;
        this.url = url;
        this.executingInstructions = 0;
        this.requests = new ConcurrentHashMap<>();
        this.startingTime = LocalDateTime.now();
    }

    public void newRequest(Request request) {
        synchronized (this) {
            String id = request.getId();
            requests.put(id, request);

            executingInstructions += request.getCost();
            request.retry();
        }
    }

    public boolean finishRequest(String requestID) {
        boolean result = false;

        synchronized (this) {
            Request request = requests.remove(requestID);
            if (request != null) {
                executingInstructions -= request.getCost();
                result = true;
            }
        }

        return result;
    }

    public boolean isRunning(){
        return this.pending == false;
    }

    public void setRunning(){
        this.pending = false;
    }

    public boolean hasRequests() {
        return this.requests.size() > 0;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getStartingTime() {
        return startingTime;
    }

    public void updateCPUAvg(Double newAvg) {
        this.cpuAvg = newAvg;
    }

    public double getCPUAvg() {
        return this.cpuAvg;
    }

    public long getExecutingInstructions() {
        synchronized (this) {
            return executingInstructions;
        }
    }

    public String getUrl() {
        return url;
    }
}
