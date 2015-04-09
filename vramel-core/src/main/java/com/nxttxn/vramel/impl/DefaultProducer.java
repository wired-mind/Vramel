package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.ExchangePattern;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.support.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DefaultProducer extends ServiceSupport implements Producer {
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

    public Exchange createExchange() {
        return endpoint.createExchange();
    }

    public Exchange createExchange(ExchangePattern pattern) {
        return endpoint.createExchange(pattern);
    }

    public Exchange createExchange(Exchange exchange) {
        return endpoint.createExchange(exchange);
    }

    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        // log at debug level for singletons, for prototype scoped log at trace level to not spam logs
        if (isSingleton()) {
            logger.debug("Starting producer: {}", this);
        } else {
            logger.trace("Starting producer: {}", this);
        }
    }

    protected void doStop() throws Exception {
        // log at debug level for singletons, for prototype scoped log at trace level to not spam logs
        if (isSingleton()) {
            logger.debug("Stopping producer: {}", this);
        } else {
            logger.trace("Stopping producer: {}", this);
        }
    }
}
