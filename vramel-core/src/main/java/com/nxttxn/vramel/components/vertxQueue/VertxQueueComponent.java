package com.nxttxn.vramel.components.vertxQueue;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 5:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxQueueComponent extends DefaultComponent {
    public VertxQueueComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        String queueName = "default";
        String address = remaining;
        final String[] splitRemaining = remaining.split(":");
        if (splitRemaining.length > 1) {
            queueName = splitRemaining[0];
            address = splitRemaining[1];
        }

        final VertxQueueChannelAdapter vertxQueueChannelAdapter = new VertxQueueChannelAdapter(getVramelContext(), queueName, address, new JsonObject(parameters).copy());
        parameters.clear();
        return vertxQueueChannelAdapter;
    }
}
