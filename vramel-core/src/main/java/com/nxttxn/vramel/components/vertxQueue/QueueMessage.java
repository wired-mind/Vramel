package com.nxttxn.vramel.components.vertxQueue;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/28/13
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueueMessage  {

    private static final String handler_uri = "handler_uri";
    private static final String _body = "body";
    private static final String _headers = "headers";
    private JsonObject jsonObject;


    public String toJson() {
        return jsonObject.toString();
    }
    public QueueMessage(String msg) {
        jsonObject = new JsonObject(msg);
    }

    public QueueMessage(String handlerUri, JsonObject body, JsonObject headers) {
        jsonObject = new JsonObject().putString(handler_uri, handlerUri).putObject(_body, body).putObject(_headers, headers);
    }

    public String getHandlerUri() {
        return jsonObject.getString(handler_uri);
    }

    public JsonObject getBody() {
        return jsonObject.getObject(_body);
    }

    public String toString() {
        return jsonObject.toString();
    }

    public JsonObject getHeaders() {
        return jsonObject.getObject(_headers, new JsonObject());
    }
}
