package com.nxttxn.vramel.components.rest;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestChannelAdapter extends DefaultEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(RestChannelAdapter.class);

    private final String route;
    private final String method;
    private final JsonObject config;

    public RestChannelAdapter(VramelContext vramelContext, String route, String method, JsonObject config) {
        super(String.format("rest:%s:%s", method, route), vramelContext);

        this.route = route;
        this.method = method;
        this.config = config;
    }

    @Override
    public Consumer createConsumer(final Processor processor) {
        return new RestConsumer(this, processor);
    }

    @Override
    public Producer createProducer() {
        return new RestProducer(this);
    }


    public String getMethod() {
        return method;
    }

    public String getRoute() {
        return route;
    }

    public JsonObject getConfig() {
        return config;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
