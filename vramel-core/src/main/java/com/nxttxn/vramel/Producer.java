package com.nxttxn.vramel;

import org.apache.camel.IsSingleton;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Producer extends Processor, Service, IsSingleton {
    Endpoint getEndpoint();


    /**
     * Creates a new exchange to send to this endpoint
     *
     * @return a newly created exchange
     */
    Exchange createExchange();

    /**
     * Creates a new exchange of the given pattern to send to this endpoint
     *
     * @param pattern the exchange pattern
     * @return a newly created exchange
     */
    Exchange createExchange(ExchangePattern pattern);

    /**
     * Creates a new exchange for communicating with this exchange using the
     * given exchange to pre-populate the values of the headers and messages
     *
     * @param exchange the existing exchange
     * @return the created exchange
     * @deprecated will be removed in Camel 3.0
     */
    @Deprecated
    Exchange createExchange(Exchange exchange);
}
