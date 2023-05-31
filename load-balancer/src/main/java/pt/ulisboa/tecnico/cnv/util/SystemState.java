package pt.ulisboa.tecnico.cnv.util;

import java.util.ArrayList;

public class SystemState {
    private ArrayList<String> instances = new ArrayList<>();
    private int instanceIndex = 0;

    public SystemState() {
        this.instances.add("http://localhost:8000");
    }

    public String getInstance() {
        this.instanceIndex = (this.instanceIndex + 1) % this.instances.size();
        return this.instances.get(this.instanceIndex);
    }
}
