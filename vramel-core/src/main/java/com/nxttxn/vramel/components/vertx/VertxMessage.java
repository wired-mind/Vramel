package com.nxttxn.vramel.components.vertx;

import com.google.common.collect.Maps;
import com.nxttxn.vramel.components.vertxQueue.QueueMessage;
import org.apache.commons.lang3.SerializationUtils;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 9:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class VertxMessage implements Serializable {

    private byte[] body;
    private Map<String, Object> headers;

    public VertxMessage(byte[] body, Map<String, Object> headers) {

        this.body = body;
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public static VertxMessage create(QueueMessage queueMessage) {
        final byte[] body = queueMessage.getBody().toString().getBytes();

        Map<String, Object> headers = Maps.newHashMap();
        final JsonObject headersJson = queueMessage.getHeaders();
        for (String headerName : headersJson.toMap().keySet()) {

            final byte[] headerValue = headersJson.getBinary(headerName);

            final Serializable val = (Serializable) SerializationUtils.deserialize(headerValue);
            headers.put(headerName, val);
        }


        return new VertxMessage(body, headers);
    }
}
