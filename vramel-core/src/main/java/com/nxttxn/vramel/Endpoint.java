package com.nxttxn.vramel;

import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Endpoint extends VramelContextAware {
    Consumer createConsumer(AsyncProcessor processor) throws Exception;
    Producer createProducer() throws Exception;

    JsonObject getConfig();

    String getEndpointUri();

    Exchange createExchange();
    /**
     * Create a new exchange for communicating with this endpoint
     * with the specified {@link ExchangePattern} such as whether its going
     * to be an {@link ExchangePattern#InOnly} or {@link ExchangePattern#InOut} exchange
     *
     * @param pattern the message exchange pattern for the exchange
     * @return a new exchange
     */
    Exchange createExchange(ExchangePattern pattern);

    /**
     * Creates a new exchange for communicating with this endpoint using the
     * given exchange to pre-populate the values of the headers and messages
     *
     * @param exchange given exchange to use for pre-populate
     * @return a new exchange
     * @deprecated will be removed in Camel 3.0
     */
    @Deprecated
    Exchange createExchange(Exchange exchange);

}
