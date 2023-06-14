package pt.ulisboa.tecnico.cnv.requests;

import com.sun.net.httpserver.HttpExchange;

public class CompressRequest extends Request {

    private byte[] responseBody;

    public CompressRequest(byte[] responseBody, long cost, HttpExchange client) {
        super(cost, client);
        this.responseBody = responseBody;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }
}
