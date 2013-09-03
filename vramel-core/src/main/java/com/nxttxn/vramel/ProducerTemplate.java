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
package com.nxttxn.vramel;

import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.Synchronization;
import org.vertx.java.core.*;
import org.vertx.java.core.Handler;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Template for working with Camel and sending {@link Message} instances in an
 * {@link Exchange} to an {@link Endpoint}.
 * <br/>
 * <p/><b>Important:</b> Read the javadoc of each method carefully to ensure the behavior of the method is understood.
 * Some methods is for <tt>InOnly</tt>, others for <tt>InOut</tt> MEP. And some methods throws
 * {@link VramelExecutionException} while others stores any thrown exception on the returned
 * {@link Exchange}.
 * <br/>
 * <p/>The {@link ProducerTemplate} is <b>thread safe</b>.
 * <br/>
 * <p/>All the methods which sends a message may throw {@link FailedToCreateProducerException} in
 * case the {@link Producer} could not be created. Or a {@link NoSuchEndpointException} if the endpoint could
 * not be resolved. There may be other related exceptions being thrown which occurs <i>before</i> the {@link Producer}
 * has started sending the message.
 * <br/>
 * <p/>All the sendBody or requestBody methods will return the content according to this strategy:
 * <ul>
 *   <li>throws {@link VramelExecutionException} if processing failed <i>during</i> routing
 *       with the caused exception wrapped</li>
 *   <li>The <tt>fault.body</tt> if there is a fault message set and its not <tt>null</tt></li>
 *   <li>Either <tt>IN</tt> or <tt>OUT</tt> body according to the message exchange pattern. If the pattern is
 *   Out capable then the <tt>OUT</tt> body is returned, otherwise <tt>IN</tt>.
 * </ul>
 * <br/>
 * <p/>Before using the template it must be started.
 * And when you are done using the template, make sure to {@link #stop()} the template.
 * <br/>
 * <p/><b>Important note on usage:</b> See this
 * <a href="http://camel.apache.org/why-does-camel-use-too-many-threads-with-producertemplate.html">FAQ entry</a>
 * before using.
 *
 * @version
 */
public interface ProducerTemplate extends Service {

    /**
     * Get the {@link VramelContext}
     *
     * @return camelContext the Camel context
     */
    VramelContext getVramelContext();

    // Configuration methods
    // -----------------------------------------------------------------------

    /**
     * Gets the maximum cache size used in the backing cache pools.
     *
     * @return the maximum cache size
     */
    int getMaximumCacheSize();

    /**
     * Sets a custom maximum cache size to use in the backing cache pools.
     *
     * @param maximumCacheSize the custom maximum cache size
     */
    void setMaximumCacheSize(int maximumCacheSize);

    /**
     * Gets an approximated size of the current cached resources in the backing cache pools.
     *
     * @return the size of current cached resources
     */
    int getCurrentCacheSize();

    /**
     * Get the default endpoint to use if none is specified
     *
     * @return the default endpoint instance
     */
    Endpoint getDefaultEndpoint();

    /**
     * Sets the default endpoint to use if none is specified
     *
     * @param defaultEndpoint the default endpoint instance
     */
    void setDefaultEndpoint(Endpoint defaultEndpoint);

    /**
     * Sets the default endpoint uri to use if none is specified
     *
     *  @param endpointUri the default endpoint uri
     */
    void setDefaultEndpointUri(String endpointUri);


    void send(String endpointUri, ExchangePattern pattern, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler);

    void send(Endpoint endpoint, Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler);

    void send(Endpoint endpoint, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler);

    void send(Endpoint endpoint, ExchangePattern pattern, Processor processor, OptionalAsyncResultHandler optionalAsyncResultHandler);

    void sendBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException;

    void sendBodyAndHeaders(Endpoint endpoint, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException;

    void sendBodyAndHeaders(String endpointUri, ExchangePattern pattern, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException;

    void sendBodyAndHeaders(Endpoint endpoint, ExchangePattern pattern, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler) throws VramelExecutionException;

    void requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler);

    void requestBodyAndHeaders(Endpoint endpoint, Object body, Map<String, Object> headers, AsyncResultHandler<Object> handler);

    <T> void requestBodyAndHeaders(String endpointUri, Object body, Map<String, Object> headers, Class<T> type, Handler<T> handler);
}
