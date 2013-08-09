package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.components.vertx.VertxProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DefaultProducer implements Producer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Endpoint endpoint;

    public DefaultProducer(Endpoint endpoint) {

        this.endpoint = endpoint;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
