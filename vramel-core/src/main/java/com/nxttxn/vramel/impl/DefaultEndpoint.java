package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.*;
import org.vertx.java.core.json.JsonObject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DefaultEndpoint implements Endpoint {
    private String endpointUri;
    private Component component;
    private VramelContext vramelContext;
    private JsonObject config;

    public DefaultEndpoint(String endpointUri, VramelContext vramelContext, JsonObject config) {
        checkNotNull(config);
        this.endpointUri = endpointUri;
        this.vramelContext = vramelContext;
        this.config = config;
    }

    protected DefaultEndpoint() {
    }

    public DefaultEndpoint(String endpointUri) {

        this.endpointUri = endpointUri;
    }

    public DefaultEndpoint(String endpointUri, Component component) {
        this.endpointUri = endpointUri;
        this.component = component;
    }


    @Override
    public VramelContext getVramelContext() {
        return vramelContext;
    }

    @Override
    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    @Override
    public JsonObject getConfig() {
        return config;
    }

    @Override
    public String getEndpointUri() {
        return endpointUri;
    }



    public Exchange createExchange(Exchange exchange) {
        return exchange.copy();
    }

    public Exchange createExchange() {
        return createExchange(ExchangePattern.InOut);
    }

    public Exchange createExchange(ExchangePattern pattern) {
        return new DefaultExchange(this, pattern);
    }



    /**
     * Sets the endpointUri if it has not been specified yet via some kind of
     * dependency injection mechanism. This allows dependency injection
     * frameworks such as Spring or Guice to set the default endpoint URI in
     * cases where it has not been explicitly configured using the name/context
     * in which an Endpoint is created.
     */
    public void setEndpointUriIfNotSpecified(String value) {
        if (endpointUri == null) {
            setEndpointUri(value);
        }
    }

    /**
     * Sets the URI that created this endpoint.
     */
    protected void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }
}
