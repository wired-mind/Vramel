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
    Consumer createConsumer(Processor processor) throws Exception;
    Producer createProducer() throws Exception;

    JsonObject getConfig();

    String getEndpointUri();

    Exchange createExchange();
}
