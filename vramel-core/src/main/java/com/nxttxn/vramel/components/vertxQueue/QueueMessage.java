package com.nxttxn.vramel.components.vertxQueue;

import org.apache.commons.lang3.SerializationUtils;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/28/13
 * Time: 9:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class QueueMessage {

    private static final String handler_uri = "handler_uri";
    private static final String _body = "body";
    private static final String _headers = "headers";
    private JsonObject jsonObject;

    public QueueMessage(JsonObject jsonObject) {

        this.jsonObject = jsonObject;
    }

    public static QueueMessage create(JsonObject body, final Map<String, Object> headers) {
        return new QueueMessage(new JsonObject()
                .putObject("body", body)
                .putObject("headers", new JsonObject() {{
                    for (Map.Entry<String, Object> header : headers.entrySet()) {
                        final Object value = header.getValue();
                        if (value instanceof Serializable) {
                            putBinary(header.getKey(), SerializationUtils.serialize((Serializable) value));
                        }
                    }
                }}));
    }


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

    public JsonObject asJson() {
        return jsonObject;
    }
}
