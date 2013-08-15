package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Consumer;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultConsumer extends ServiceSupport implements Consumer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Endpoint endpoint;
    private final AsyncProcessor processor;

    public DefaultConsumer(Endpoint endpoint, AsyncProcessor processor) {

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

    protected void doStop() throws Exception {
        logger.debug("Stopping consumer: {}", this);
        ServiceHelper.stopServices(processor);
    }

    protected void doStart() throws Exception {
        logger.debug("Starting consumer: {}", this);
        ServiceHelper.startServices(processor);
    }
    public AsyncProcessor getProcessor() {
        return processor;
    }
}
