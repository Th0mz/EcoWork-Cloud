package pt.ulisboa.tecnico.cnv.util;

public class InstanceState {

    private String id;
    private String url;
    private Double cpuAvg = null;
    private long executingInstructions ;

    public InstanceState(String id, String url) {
        this.id = id;
        this.url = url;
        this.executingInstructions = 0;
    }

    public String getId() {
        return id;
    }

    public void updateCPUAvg(Double newAvg) {
        this.cpuAvg = newAvg;
    }

    public long getExecutingInstructions() {
        synchronized (this) {
            return executingInstructions;
        }
    }
    public void addInstructions(long instructions) {
        synchronized (this) {
            this.executingInstructions += instructions;
        }
    }

    public void removeInstruction(long instructions) {
        synchronized (this) {
            this.executingInstructions -= instructions;
        }
    }

    public String getUrl() {
        return url;
    }
}
