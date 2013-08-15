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
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.Synchronization;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.ExchangeHelper;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import com.nxttxn.vramel.util.VramelContextHelper;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;



/**
 * Template (named like Spring's TransactionTemplate & JmsTemplate
 * et al) for working with Camel and sending {@link Message} instances in an
 * {@link Exchange} to an {@link Endpoint}.
 *
 * @version
 */
public class DefaultProducerTemplate extends ServiceSupport implements ProducerTemplate {
    private final VramelContext vramelContext;
    private volatile ProducerCache producerCache;
    private volatile ExecutorService executor;
    private Endpoint defaultEndpoint;
    private int maximumCacheSize;

    public DefaultProducerTemplate(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    public DefaultProducerTemplate(VramelContext vramelContext, ExecutorService executor) {
        this.vramelContext = vramelContext;
        this.executor = executor;
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

    public Exchange send(String endpointUri, Exchange exchange) {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        return send(endpoint, exchange);
    }

    public Exchange send(String endpointUri, Processor processor) {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        return send(endpoint, processor);
    }

    public Exchange send(String endpointUri, ExchangePattern pattern, Processor processor) {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        return send(endpoint, pattern, processor);
    }

    public Exchange send(Endpoint endpoint, Exchange exchange) {
        getProducerCache().send(endpoint, exchange);
        return exchange;
    }

    public Exchange send(Endpoint endpoint, Processor processor) {
        return getProducerCache().send(endpoint, processor);
    }

    public Exchange send(Endpoint endpoint, ExchangePattern pattern, Processor processor) {
        return getProducerCache().send(endpoint, pattern, processor);
    }

    public Object sendBody(Endpoint endpoint, ExchangePattern pattern, Object body) {
        Exchange result = send(endpoint, pattern, createSetBodyProcessor(body));
        return extractResultBody(result, pattern);
    }

    public void sendBody(Endpoint endpoint, Object body) throws VramelExecutionException {
        Exchange result = send(endpoint, createSetBodyProcessor(body));
        // must invoke extract result body in case of exception to be rethrown
        extractResultBody(result);
    }

    public void sendBody(String endpointUri, Object body) throws VramelExecutionException {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        sendBody(endpoint, body);
    }

    public Object sendBody(String endpointUri, ExchangePattern pattern, Object body) throws VramelExecutionException {
        Endpoint endpoint = resolveMandatoryEndpoint(endpointUri);
        Object result = sendBody(endpoint, pattern, body);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    public void sendBodyAndHeader(String endpointUri, final Object body, final String header, final Object headerValue) throws VramelExecutionException {
        sendBodyAndHeader(resolveMandatoryEndpoint(endpointUri), body, header, headerValue);
    }

    public void sendBodyAndHeader(Endpoint endpoint, final Object body, final String header, final Object headerValue) throws VramelExecutionException {
        Exchange result = send(endpoint, createBodyAndHeaderProcessor(body, header, headerValue));
        // must invoke extract result body in case of exception to be rethrown
        extractResultBody(result);
    }

    public Object sendBodyAndHeader(Endpoint endpoint, ExchangePattern pattern, final Object body,
                                    final String header, final Object headerValue) throws VramelExecutionException {
        Exchange exchange = send(endpoint, pattern, createBodyAndHeaderProcessor(body, header, headerValue));
        Object result = extractResultBody(exchange, pattern);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    public Object sendBodyAndHeader(String endpoint, ExchangePattern pattern, final Object body,
                                    final String header, final Object headerValue) throws VramelExecutionException {
        Exchange exchange = send(endpoint, pattern, createBodyAndHeaderProcessor(body, header, headerValue));
        Object result = extractResultBody(exchange, pattern);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    public void sendBodyAndProperty(String endpointUri, final Object body,
                                    final String property, final Object propertyValue) throws VramelExecutionException {
        sendBodyAndProperty(resolveMandatoryEndpoint(endpointUri), body, property, propertyValue);
    }

    public void sendBodyAndProperty(Endpoint endpoint, final Object body,
                                    final String property, final Object propertyValue) throws VramelExecutionException {
        Exchange result = send(endpoint, createBodyAndPropertyProcessor(body, property, propertyValue));
        // must invoke extract result body in case of exception to be rethrown
        extractResultBody(result);
    }

    public Object sendBodyAndProperty(Endpoint endpoint, ExchangePattern pattern, final Object body,
                                      final String property, final Object propertyValue) throws VramelExecutionException {
        Exchange exchange = send(endpoint, pattern, createBodyAndPropertyProcessor(body, property, propertyValue));
        Object result = extractResultBody(exchange, pattern);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    public Object sendBodyAndProperty(String endpoint, ExchangePattern pattern, final Object body,
                                      final String property, final Object propertyValue) throws VramelExecutionException {
        Exchange exchange = send(endpoint, pattern, createBodyAndPropertyProcessor(body, property, propertyValue));
        Object result = extractResultBody(exchange, pattern);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    public void sendBodyAndHeaders(String endpointUri, final Object body, final Map<String, Object> headers) throws VramelExecutionException {
        sendBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers);
    }

    public void sendBodyAndHeaders(Endpoint endpoint, final Object body, final Map<String, Object> headers) throws VramelExecutionException {
        Exchange result = send(endpoint, new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    in.setHeader(header.getKey(), header.getValue());
                }
                in.setBody(body);
            }
        });
        // must invoke extract result body in case of exception to be rethrown
        extractResultBody(result);
    }

    public Object sendBodyAndHeaders(String endpointUri, ExchangePattern pattern, Object body, Map<String, Object> headers) throws VramelExecutionException {
        return sendBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), pattern, body, headers);
    }

    public Object sendBodyAndHeaders(Endpoint endpoint, ExchangePattern pattern, final Object body, final Map<String, Object> headers) throws VramelExecutionException {
        Exchange exchange = send(endpoint, pattern, new Processor() {
            public void process(Exchange exchange) throws Exception {
                Message in = exchange.getIn();
                for (Map.Entry<String, Object> header : headers.entrySet()) {
                    in.setHeader(header.getKey(), header.getValue());
                }
                in.setBody(body);
            }
        });
        Object result = extractResultBody(exchange, pattern);
        if (pattern.isOutCapable()) {
            return result;
        } else {
            // return null if not OUT capable
            return null;
        }
    }

    // Methods using an InOut ExchangePattern
    // -----------------------------------------------------------------------

    public Exchange request(Endpoint endpoint, Processor processor) {
        return send(endpoint, ExchangePattern.InOut, processor);
    }

    public Object requestBody(Object body) throws VramelExecutionException {
        return sendBody(getMandatoryDefaultEndpoint(), ExchangePattern.InOut, body);
    }

    public Object requestBody(Endpoint endpoint, Object body) throws VramelExecutionException {
        return sendBody(endpoint, ExchangePattern.InOut, body);
    }

    public Object requestBodyAndHeader(Object body, String header, Object headerValue) throws VramelExecutionException {
        return sendBodyAndHeader(getMandatoryDefaultEndpoint(), ExchangePattern.InOut, body, header, headerValue);
    }

    public Object requestBodyAndHeader(Endpoint endpoint, Object body, String header, Object headerValue) throws VramelExecutionException {
        return sendBodyAndHeader(endpoint, ExchangePattern.InOut, body, header, headerValue);
    }

    public Exchange request(String endpoint, Processor processor) throws VramelExecutionException {
        return send(endpoint, ExchangePattern.InOut, processor);
    }

    public Object requestBody(String endpoint, Object body) throws VramelExecutionException {
        return sendBody(endpoint, ExchangePattern.InOut, body);
    }

    public Object requestBodyAndHeader(String endpoint, Object body, String header, Object headerValue) throws VramelExecutionException {
        return sendBodyAndHeader(endpoint, ExchangePattern.InOut, body, header, headerValue);
    }

    public Object requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers) {
        return requestBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers);
    }

    public Object requestBodyAndHeaders(Endpoint endpoint, final Object body, final Map<String, Object> headers) {
        return sendBodyAndHeaders(endpoint, ExchangePattern.InOut, body, headers);
    }

    public Object requestBodyAndHeaders(final Object body, final Map<String, Object> headers) {
        return sendBodyAndHeaders(getDefaultEndpoint(), ExchangePattern.InOut, body, headers);
    }

    public <T> T requestBody(Object body, Class<T> type) {
        Object answer = requestBody(body);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBody(Endpoint endpoint, Object body, Class<T> type) {
        Object answer = requestBody(endpoint, body);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBody(String endpointUri, Object body, Class<T> type) {
        Object answer = requestBody(endpointUri, body);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBodyAndHeader(Endpoint endpoint, Object body, String header, Object headerValue, Class<T> type) {
        Object answer = requestBodyAndHeader(endpoint, body, header, headerValue);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBodyAndHeader(String endpointUri, Object body, String header, Object headerValue, Class<T> type) {
        Object answer = requestBodyAndHeader(endpointUri, body, header, headerValue);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, Class<T> type) {
        Object answer = requestBodyAndHeaders(endpointUri, body, headers);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    public <T> T requestBodyAndHeaders(Endpoint endpoint, Object body, Map<String, Object> headers, Class<T> type) {
        Object answer = requestBodyAndHeaders(endpoint, body, headers);
        return vramelContext.getTypeConverter().convertTo(type, answer);
    }

    // Methods using the default endpoint
    // -----------------------------------------------------------------------

    public void sendBody(Object body) {
        sendBody(getMandatoryDefaultEndpoint(), body);
    }

    public Exchange send(Exchange exchange) {
        return send(getMandatoryDefaultEndpoint(), exchange);
    }

    public Exchange send(Processor processor) {
        return send(getMandatoryDefaultEndpoint(), processor);
    }

    public void sendBodyAndHeader(Object body, String header, Object headerValue) {
        sendBodyAndHeader(getMandatoryDefaultEndpoint(), body, header, headerValue);
    }

    public void sendBodyAndProperty(Object body, String property, Object propertyValue) {
        sendBodyAndProperty(getMandatoryDefaultEndpoint(), body, property, propertyValue);
    }

    public void sendBodyAndHeaders(Object body, Map<String, Object> headers) {
        sendBodyAndHeaders(getMandatoryDefaultEndpoint(), body, headers);
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

    public void setExecutorService(ExecutorService executorService) {
        this.executor = executorService;
    }

    public Future<Exchange> asyncSend(final String uri, final Exchange exchange) {
        return asyncSend(resolveMandatoryEndpoint(uri), exchange);
    }

    public Future<Exchange> asyncSend(final String uri, final Processor processor) {
        return asyncSend(resolveMandatoryEndpoint(uri), processor);
    }

    public Future<Object> asyncSendBody(final String uri, final Object body) {
        return asyncSendBody(resolveMandatoryEndpoint(uri), body);
    }

    public Future<Object> asyncRequestBody(final String uri, final Object body) {
        return asyncRequestBody(resolveMandatoryEndpoint(uri), body);
    }

    public <T> Future<T> asyncRequestBody(final String uri, final Object body, final Class<T> type) {
        return asyncRequestBody(resolveMandatoryEndpoint(uri), body, type);
    }

    public Future<Object> asyncRequestBodyAndHeader(final String endpointUri, final Object body, final String header, final Object headerValue) {
        return asyncRequestBodyAndHeader(resolveMandatoryEndpoint(endpointUri), body, header, headerValue);
    }

    public <T> Future<T> asyncRequestBodyAndHeader(final String endpointUri, final Object body, final String header, final Object headerValue, final Class<T> type) {
        return asyncRequestBodyAndHeader(resolveMandatoryEndpoint(endpointUri), body, header, headerValue, type);
    }

    public Future<Object> asyncRequestBodyAndHeaders(final String endpointUri, final Object body, final Map<String, Object> headers) {
        return asyncRequestBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers);
    }

    public <T> Future<T> asyncRequestBodyAndHeaders(final String endpointUri, final Object body, final Map<String, Object> headers, final Class<T> type) {
        return asyncRequestBodyAndHeaders(resolveMandatoryEndpoint(endpointUri), body, headers, type);
    }

    public <T> T extractFutureBody(Future<Object> future, Class<T> type) {
        return ExchangeHelper.extractFutureBody(vramelContext, future, type);
    }

    public <T> T extractFutureBody(Future<Object> future, long timeout, TimeUnit unit, Class<T> type) throws TimeoutException {
        return ExchangeHelper.extractFutureBody(vramelContext, future, timeout, unit, type);
    }

    public Future<Object> asyncCallbackSendBody(String uri, Object body, Synchronization onCompletion) {
        return asyncCallbackSendBody(resolveMandatoryEndpoint(uri), body, onCompletion);
    }

    public Future<Object> asyncCallbackSendBody(Endpoint endpoint, Object body, Synchronization onCompletion) {
        return asyncCallback(endpoint, ExchangePattern.InOnly, body, onCompletion);
    }

    public Future<Object> asyncCallbackRequestBody(String uri, Object body, Synchronization onCompletion) {
        return asyncCallbackRequestBody(resolveMandatoryEndpoint(uri), body, onCompletion);
    }

    public Future<Object> asyncCallbackRequestBody(Endpoint endpoint, Object body, Synchronization onCompletion) {
        return asyncCallback(endpoint, ExchangePattern.InOut, body, onCompletion);
    }

    public Future<Exchange> asyncCallback(String uri, Exchange exchange, Synchronization onCompletion) {
        return asyncCallback(resolveMandatoryEndpoint(uri), exchange, onCompletion);
    }

    public Future<Exchange> asyncCallback(String uri, Processor processor, Synchronization onCompletion) {
        return asyncCallback(resolveMandatoryEndpoint(uri), processor, onCompletion);
    }

    public Future<Object> asyncRequestBody(final Endpoint endpoint, final Object body) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() throws Exception {
                return requestBody(endpoint, body);
            }
        };
        return getExecutorService().submit(task);
    }

    public <T> Future<T> asyncRequestBody(final Endpoint endpoint, final Object body, final Class<T> type) {
        Callable<T> task = new Callable<T>() {
            public T call() throws Exception {
                return requestBody(endpoint, body, type);
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Object> asyncRequestBodyAndHeader(final Endpoint endpoint, final Object body, final String header,
                                                    final Object headerValue) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() throws Exception {
                return requestBodyAndHeader(endpoint, body, header, headerValue);
            }
        };
        return getExecutorService().submit(task);
    }

    public <T> Future<T> asyncRequestBodyAndHeader(final Endpoint endpoint, final Object body, final String header,
                                                   final Object headerValue, final Class<T> type) {
        Callable<T> task = new Callable<T>() {
            public T call() throws Exception {
                return requestBodyAndHeader(endpoint, body, header, headerValue, type);
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Object> asyncRequestBodyAndHeaders(final Endpoint endpoint, final Object body,
                                                     final Map<String, Object> headers) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() throws Exception {
                return requestBodyAndHeaders(endpoint, body, headers);
            }
        };
        return getExecutorService().submit(task);
    }

    public <T> Future<T> asyncRequestBodyAndHeaders(final Endpoint endpoint, final Object body,
                                                    final Map<String, Object> headers, final Class<T> type) {
        Callable<T> task = new Callable<T>() {
            public T call() throws Exception {
                return requestBodyAndHeaders(endpoint, body, headers, type);
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Exchange> asyncSend(final Endpoint endpoint, final Exchange exchange) {
        Callable<Exchange> task = new Callable<Exchange>() {
            public Exchange call() throws Exception {
                return send(endpoint, exchange);
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Exchange> asyncSend(final Endpoint endpoint, final Processor processor) {
        Callable<Exchange> task = new Callable<Exchange>() {
            public Exchange call() throws Exception {
                return send(endpoint, processor);
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Object> asyncSendBody(final Endpoint endpoint, final Object body) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() throws Exception {
                sendBody(endpoint, body);
                // its InOnly, so no body to return
                return null;
            }
        };
        return getExecutorService().submit(task);
    }

    private Future<Object> asyncCallback(final Endpoint endpoint, final ExchangePattern pattern, final Object body, final Synchronization onCompletion) {
        Callable<Object> task = new Callable<Object>() {
            public Object call() throws Exception {
                Exchange answer = send(endpoint, pattern, createSetBodyProcessor(body));

                // invoke callback before returning answer
                // as it allows callback to be used without UnitOfWorkProcessor invoking it
                // and thus it works directly from a producer template as well, as opposed
                // to the UnitOfWorkProcessor that is injected in routes
                if (answer.isFailed()) {
                    onCompletion.onFailure(answer);
                } else {
                    onCompletion.onComplete(answer);
                }

                Object result = extractResultBody(answer, pattern);
                if (pattern.isOutCapable()) {
                    return result;
                } else {
                    // return null if not OUT capable
                    return null;
                }
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Exchange> asyncCallback(final Endpoint endpoint, final Exchange exchange, final Synchronization onCompletion) {
        Callable<Exchange> task = new Callable<Exchange>() {
            public Exchange call() throws Exception {
                // process the exchange, any exception occurring will be caught and set on the exchange
                send(endpoint, exchange);

                // invoke callback before returning answer
                // as it allows callback to be used without UnitOfWorkProcessor invoking it
                // and thus it works directly from a producer template as well, as opposed
                // to the UnitOfWorkProcessor that is injected in routes
                if (exchange.isFailed()) {
                    onCompletion.onFailure(exchange);
                } else {
                    onCompletion.onComplete(exchange);
                }
                return exchange;
            }
        };
        return getExecutorService().submit(task);
    }

    public Future<Exchange> asyncCallback(final Endpoint endpoint, final Processor processor, final Synchronization onCompletion) {
        Callable<Exchange> task = new Callable<Exchange>() {
            public Exchange call() throws Exception {
                // process the exchange, any exception occurring will be caught and set on the exchange
                Exchange answer = send(endpoint, processor);

                // invoke callback before returning answer
                // as it allows callback to be used without UnitOfWorkProcessor invoking it
                // and thus it works directly from a producer template as well, as opposed
                // to the UnitOfWorkProcessor that is injected in routes
                if (answer.isFailed()) {
                    onCompletion.onFailure(answer);
                } else {
                    onCompletion.onComplete(answer);
                }
                return answer;
            }
        };
        return getExecutorService().submit(task);
    }

    private ProducerCache getProducerCache() {
        if (!isStarted()) {
            throw new IllegalStateException("ProducerTemplate has not been started");
        }
        return producerCache;
    }

    private ExecutorService getExecutorService() {
        if (!isStarted()) {
            throw new IllegalStateException("ProducerTemplate has not been started");
        }

        if (executor != null) {
            return executor;
        }

        // create a default executor which must be synchronized
        synchronized (this) {
            if (executor != null) {
                return executor;
            }
//            executor = vramelContext.getExecutorServiceManager().newDefaultThreadPool(this, "ProducerTemplate");
        }

        ObjectHelper.notNull(executor, "ExecutorService");
        return executor;
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

        if (executor != null) {
//            vramelContext.getExecutorServiceManager().shutdownNow(executor);
            executor = null;
        }
    }

}
