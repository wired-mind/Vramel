package com.nxttxn.vramel.components.vertxQueue;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 5:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxQueueChannelAdapter extends DefaultEndpoint {


    private final String queueName;
    private final String address;

    public VertxQueueChannelAdapter(VramelContext vramelContext, String queueName, String address, JsonObject config) {
        super(String.format("vertxQueue:%s:%s", queueName, address), vramelContext, config);
        this.queueName = queueName;
        this.address = address;
    }

    @Override
    public Consumer createConsumer(AsyncProcessor processor) throws Exception {
        return new VertxQueueConsumer(this, processor);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new VertxQueueProducer(this, getAddress());
    }

    public String getAddress() {
        return address;
    }

    public String getQueueName() {
        return queueName;
    }
}
