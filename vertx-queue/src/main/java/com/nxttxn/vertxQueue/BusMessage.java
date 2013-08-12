package com.nxttxn.vertxQueue;

import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/28/13
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class BusMessage extends JsonObject {

    private static final String handler_uri = "handler_uri";
    private static final String _body = "body";

    public BusMessage(String body) {
        super(body);
    }


    public String getHandlerUri() {
        return getString(handler_uri);
    }

    public JsonObject getBody() {
        return getObject(_body);
    }


}
