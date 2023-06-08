package pt.ulisboa.tecnico.cnv.util;

public class InstanceState {
    private String url;
    private Double cpuAvg = null;

    public InstanceState(String url) {
        this.url = url;
    }

    public void updateCPUAvg(Double newAvg) {
        this.cpuAvg = newAvg;
    }

    public String getUrl() {
        return url;
    }
}
