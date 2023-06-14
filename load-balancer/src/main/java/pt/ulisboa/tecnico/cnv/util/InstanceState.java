package pt.ulisboa.tecnico.cnv.util;

import pt.ulisboa.tecnico.cnv.requests.Request;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceState {

    private String id;
    private String url;
    private Double cpuAvg = null;
    private long executingInstructions;
    private ConcurrentHashMap<String, Request> requests;


    public InstanceState(String id, String url) {
        this.id = id;
        this.url = url;
        this.executingInstructions = 0;
        this.requests = new ConcurrentHashMap<>();
    }

    public void newRequest(Request request) {
        synchronized (this) {
            String id = request.getId();
            requests.put(id, request);

            executingInstructions += request.getCost();
            request.retry();
        }
    }

    public void finishRequest(String requestID) {
        synchronized (this) {
            Request request = requests.remove(requestID);
            if (request != null) {
                executingInstructions -= request.getCost();
            }
        }
    }

    public boolean hasRequests() {
        return this.requests.size() > 0;
    }

    public String getId() {
        return id;
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
