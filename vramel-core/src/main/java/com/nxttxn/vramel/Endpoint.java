package com.nxttxn.vramel;

import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Endpoint extends IsSingleton, Service, VramelContextAware {
    Consumer createConsumer(Processor processor) throws Exception;
    Producer createProducer() throws Exception;


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

    /**
     * Returns a string key of this endpoint.
     * <p/>
     * This key is used by {@link org.apache.camel.spi.LifecycleStrategy} when registering endpoint.
     * This allows to register different instances of endpoints with the same key.
     * <p/>
     * For JMX mbeans this allows us to use the same JMX Mbean for all endpoints that are logical
     * the same but have different parameters. For instance the http endpoint.
     *
     * @return the endpoint key
     */
    String getEndpointKey();

    /**
     * Configure properties on this endpoint.
     *
     * @param options  the options (properties)
     */
    void configureProperties(Map<String, Object> options);

    /**
     * Should all properties be known or does the endpoint allow unknown options?
     * <p/>
     * <tt>lenient = false</tt> means that the endpoint should validate that all
     * given options is known and configured properly.
     * <tt>lenient = true</tt> means that the endpoint allows additional unknown options to
     * be passed to it but does not throw a ResolveEndpointFailedException when creating
     * the endpoint.
     * <p/>
     * This options is used by a few components for instance the HTTP based that can have
     * dynamic URI options appended that is targeted for an external system.
     * <p/>
     * Most endpoints is configured to be <b>not</b> lenient.
     *
     * @return whether properties is lenient or not
     */
    boolean isLenientProperties();


}
