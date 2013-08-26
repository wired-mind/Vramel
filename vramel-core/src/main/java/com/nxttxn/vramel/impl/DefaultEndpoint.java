package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.IntrospectionSupport;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.URISupport;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class DefaultEndpoint extends ServiceSupport implements Endpoint {
    private String endpointUri;
    private Component component;
    private VramelContext vramelContext;
    private Map<String, Object> consumerProperties;

    public DefaultEndpoint(String endpointUri, VramelContext vramelContext) {
        this.endpointUri = endpointUri;
        this.vramelContext = vramelContext;
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

    public int hashCode() {
        return getEndpointUri().hashCode() * 37 + 1;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DefaultEndpoint) {
            DefaultEndpoint that = (DefaultEndpoint)object;
            return ObjectHelper.equal(this.getEndpointUri(), that.getEndpointUri());
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("Endpoint[%s]", URISupport.sanitizeUri(getEndpointUri()));
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

    public String getEndpointKey() {
        if (isLenientProperties()) {
            // only use the endpoint uri without parameters as the properties is
            // lenient
            String uri = getEndpointUri();
            if (uri.indexOf('?') != -1) {
                return ObjectHelper.before(uri, "?");
            } else {
                return uri;
            }
        } else {
            // use the full endpoint uri
            return getEndpointUri();
        }
    }

    public boolean isLenientProperties() {
        // default should be false for most components
        return true; //temporarily return true until we sort out uri params vs. jsonobject params
    }

    public void configureProperties(Map<String, Object> options) {
        Map<String, Object> consumerProperties = IntrospectionSupport.extractProperties(options, "consumer.");
        if (consumerProperties != null) {
            setConsumerProperties(consumerProperties);
        }
    }

    public void setConsumerProperties(Map<String, Object> consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public Map<String, Object> getConsumerProperties() {
        return consumerProperties;
    }


    @Override
    protected void doStart() throws Exception {
        // noop
    }

    @Override
    protected void doStop() throws Exception {
        // noop
    }
}
