package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.Component;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.VramelContext;
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

    @Override
    public Exchange createExchange() {
        return new DefaultExchange(vramelContext);
    }


}
