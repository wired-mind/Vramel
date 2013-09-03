/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.ExchangeHelper;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import com.nxttxn.vramel.util.VramelContextHelper;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;

import java.util.Map;


/**
 * Template (named like Spring's TransactionTemplate & JmsTemplate
 * et al) for working with Camel and sending {@link Message} instances in an
 * {@link Exchange} to an {@link Endpoint}.
 */
public class DefaultProducerTemplate extends ServiceSupport implements ProducerTemplate {
    private final VramelContext vramelContext;
    private volatile ProducerCache producerCache;
    private Endpoint defaultEndpoint;
    private int maximumCacheSize;

    public DefaultProducerTemplate(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }


    public DefaultProducerTemplate(VramelContext vramelContext, Endpoint defaultEndpoint) {
        this(vramelContext);
        this.defaultEndpoint = defaultEndpoint;
    }

    public static DefaultProducerTemplate newInstance(VramelContext camelContext, String defaultEndpointUri) {
        Endpoint endpoint = VramelContextHelper.getMandatoryEndpoint(camelContext, defaultEndpointUri);
        return new DefaultProducerTemplate(camelContext, endpoint);
    }

    public int getMaximumCacheSize() {
        return maximumCacheSize;
    }

    public void setMaximumCacheSize(int maximumCacheSize) {
        this.maximumCacheSize = maximumCacheSize;
    }

    public int getCurrentCacheSize() {
        if (producerCache == null) {
            return 0;
        }
        return producerCache.size();
    }



    @Override
    public void send(String endpointUri, ExchangePattern pattern, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        send(endpoint, pattern, processor, optionalAsyncResultHandler);
    }

    @Override
    public void send(Endpoint endpoint, Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        getProducerCache().send(endpoint, exchange, optionalAsyncResultHandler);
    }

    @Override
    public void send(Endpoint endpoint, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        getProducerCache().send(endpoint, processor, optionalAsyncResultHandler);
    }

    @Override
    public void send(Endpoint endpoint, ExchangePattern pattern, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        getProducerCache().send(endpoint, pattern, processor, optionalAsyncResultHandler);
    }


    @Override
    public void sendBodyAndHeaders(String endpointUri, final Object body, final Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException {
        sendBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers, handler);
    }

    @Override
    public void sendBodyAndHeaders(Endpoint endpoint, final Object body, final Map<String, Object> headers, final AsyncResultHandler<Object> handler) throws VramelExecutionException {
        send(endpoint, new Processor() {
                    public void process(Exchange exchange) {
                        Message in = exchange.getIn();
                        for (Map.Entry<String, Object> header : headers.entrySet()) {
                            in.setHeader(header.getKey(), header.getValue());
                        }
                        in.setBody(body);
                    }
                }, new OptionalAsyncResultHandler() {
                    @Override
                    public void handle(AsyncExchangeResult event) {
                        // must invoke extract result body in case of exception to be rethrown
                        try {
                            final Object result = extractResultBody(event.result.get());
                            handler.handle(new AsyncResult<Object>(result));
                        } catch (Exception e) {
                            handler.handle(new AsyncResult<>(e));
                        }
                    }
                }
        );

    }

    @Override
    public void sendBodyAndHeaders(String endpointUri, ExchangePattern pattern, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException {
        sendBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), pattern, body, headers, handler);
    }

    @Override
    public void sendBodyAndHeaders(Endpoint endpoint, final ExchangePattern pattern, final Object body, final Map<String, Object> headers, final AsyncResultHandler<Object> handler) throws VramelExecutionException {
        send(endpoint, pattern, new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        for (Map.Entry<String, Object> header : headers.entrySet()) {
                            in.setHeader(header.getKey(), header.getValue());
                        }
                        in.setBody(body);
                    }
                }, new OptionalAsyncResultHandler() {
                    @Override
                    public void handle(AsyncExchangeResult event) {
                        try {
                            Object result = extractResultBody(event.result.get(), pattern);
                            if (pattern.isOutCapable()) {
                                handler.handle(new AsyncResult<>(result));
                            } else {
                                // return null if not OUT capable
                                handler.handle(new AsyncResult<>(null));
                            }
                        } catch (Exception e) {
                            handler.handle(new AsyncResult<>(e));
                        }

                    }
                }
        );

    }

    // Methods using an InOut ExchangePattern
    // -----------------------------------------------------------------------


    @Override
    public void requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) {
        requestBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers, handler);
    }

    @Override
    public void requestBodyAndHeaders(Endpoint endpoint, final Object body, final Map<String, Object> headers, AsyncResultHandler<Object> handler) {
        sendBodyAndHeaders(endpoint, ExchangePattern.InOut, body, headers, handler);
    }


    @Override
    public <T> void requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, final Class<T> type, final Handler<T> handler) {
        requestBodyAndHeaders(endpointUri, body, headers, new AsyncResultHandler<Object>() {
            @Override
            public void handle(AsyncResult<Object> event) {
                final T result = vramelContext.getTypeConverter().convertTo(type, event.result);
                handler.handle(result);
            }
        });

    }


    // Properties
    // -----------------------------------------------------------------------

    /**
     * @deprecated use {@link #getVramelContext()}
     */
    @Deprecated
    public VramelContext getContext() {
        return getVramelContext();
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public Endpoint getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public void setDefaultEndpoint(Endpoint defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;
    }

    /**
     * Sets the default endpoint to use if none is specified
     */
    public void setDefaultEndpointUri(String endpointUri) {
        setDefaultEndpoint(getVramelContext().getEndpoint(endpointUri));
    }

    /**
     * @deprecated use {@link VramelContext#getEndpoint(String, Class)}
     */
    @Deprecated
    public <T extends Endpoint> T getResolvedEndpoint(String endpointUri, Class<T> expectedClass) {
        return vramelContext.getEndpoint(endpointUri, expectedClass);
    }

    // Implementation methods
    // -----------------------------------------------------------------------

    protected Processor createBodyAndHeaderProcessor(final Object body, final String header, final Object headerValue) {
        return new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setHeader(header, headerValue);
                in.setBody(body);
            }
        };
    }

    protected Processor createBodyAndPropertyProcessor(final Object body, final String property, final Object propertyValue) {
        return new Processor() {
            public void process(Exchange exchange) {
                exchange.setProperty(property, propertyValue);
                Message in = exchange.getIn();
                in.setBody(body);
            }
        };
    }

    protected Processor createSetBodyProcessor(final Object body) {
        return new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setBody(body);
            }
        };
    }

    protected Endpoint resolveMandatoryEndpoint(String endpointUri) {
        Endpoint endpoint = vramelContext.getEndpoint(endpointUri);
        if (endpoint == null) {
            throw new NoSuchEndpointException(endpointUri);
        }
        return endpoint;
    }

    protected Endpoint getMandatoryDefaultEndpoint() {
        Endpoint answer = getDefaultEndpoint();
        ObjectHelper.notNull(answer, "defaultEndpoint");
        return answer;
    }

    protected Object extractResultBody(Exchange result) {
        return extractResultBody(result, null);
    }

    protected Object extractResultBody(Exchange result, ExchangePattern pattern) {
        return ExchangeHelper.extractResultBody(result, pattern);
    }


    private ProducerCache getProducerCache() {
        if (!isStarted()) {
            throw new IllegalStateException("ProducerTemplate has not been started");
        }
        return producerCache;
    }


    protected void doStart() throws Exception {
        if (producerCache == null) {
            if (maximumCacheSize > 0) {
                producerCache = new ProducerCache(this, vramelContext, maximumCacheSize);
            } else {
                producerCache = new ProducerCache(this, vramelContext);
            }
        }
        ServiceHelper.startService(producerCache);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(producerCache);
        producerCache = null;

    }

}
