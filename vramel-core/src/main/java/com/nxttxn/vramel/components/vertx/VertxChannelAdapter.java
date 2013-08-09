package com.nxttxn.vramel.components.vertx;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxChannelAdapter extends DefaultEndpoint {
    private final String address;

    public VertxChannelAdapter(VramelContext vramelContext, String address, JsonObject config) {
        super(String.format("vertx:%s", address), vramelContext, config);
        this.address = address;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new VertxConsumer(this, processor);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new VertxProducer(this, getAddress());
    }

    public String getAddress() {
        return address;
    }
}
