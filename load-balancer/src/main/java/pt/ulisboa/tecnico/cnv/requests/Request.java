package pt.ulisboa.tecnico.cnv.requests;

import com.sun.net.httpserver.HttpExchange;

import java.util.UUID;

public class Request {

    private HttpExchange client;
    private int tries;
    private Long cost;
    private String id;

    public Request (Long cost, HttpExchange client) {
        this.id = UUID.randomUUID().toString();
        this.cost = cost;
        this.client = client;
    }

    public String getId() {
        return id;
    }

    public HttpExchange getClient() {
        return client;
    }

    public int getTries() {
        return tries;
    }

    public void retry() {
        this.tries ++;
    }

    public Long getCost() {
        return cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Request)) {
            return false;
        }

        Request request = (Request) obj;
        return this.id.equals(request.getId());
    }
}
