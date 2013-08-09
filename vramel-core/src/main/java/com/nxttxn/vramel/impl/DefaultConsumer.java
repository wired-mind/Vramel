package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Consumer;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.components.rest.RestConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultConsumer implements Consumer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Endpoint endpoint;
    private final Processor processor;

    public DefaultConsumer(Endpoint endpoint, Processor processor) {

        this.endpoint = endpoint;
        this.processor = processor;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    protected void logError(Throwable ex) {

        logger.error(ex.getMessage(), ex);
        if (ex.getCause() != null) {
            logError(ex.getCause());
        }


    }

    public Processor getProcessor() {
        return processor;
    }
}
