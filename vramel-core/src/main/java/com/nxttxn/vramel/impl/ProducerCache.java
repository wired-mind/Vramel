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

import java.util.Map;


import com.nxttxn.vramel.*;
import org.apache.camel.util.LRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache containing created {@link Producer}.
 *
 * @version
 */
public class ProducerCache  {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProducerCache.class);

    private final VramelContext vramelContext;
    private final Map<String, Producer> producers;
    private final Object source;

    public ProducerCache(Object source, VramelContext vramelContext) {
        this(source, vramelContext, 100);
    }

    public ProducerCache(Object source, VramelContext vramelContext, int cacheSize) {
        this(source, vramelContext, createLRUCache(cacheSize));
    }


    public ProducerCache(Object source, VramelContext vramelContext, Map<String, Producer> cache) {
        this.source = source;
        this.vramelContext = vramelContext;
        this.producers = cache;
    }

    /**
     * Creates the {@link LRUCache} to be used.
     * <p/>
     * This implementation returns a {@link LRUCache} instance.

     * @param cacheSize the cache size
     * @return the cache
     */
    protected static LRUCache<String, Producer> createLRUCache(int cacheSize) {
        // Use a regular cache as we want to ensure that the lifecycle of the producers
        // being cache is properly handled, such as they are stopped when being evicted
        // or when this cache is stopped. This is needed as some producers requires to
        // be stopped so they can shutdown internal resources that otherwise may cause leaks
        return new LRUCache<String, Producer>(cacheSize);
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    /**
     * Gets the source which uses this cache
     *
     * @return the source
     */
    public Object getSource() {
        return source;
    }

    /**
     * @param endpoint the endpoint
     * @return the producer
     */
    public Producer acquireProducer(Endpoint endpoint) {
        return doGetProducer(endpoint, true);
    }



    protected synchronized Producer doGetProducer(Endpoint endpoint, boolean pooled) {
        String key = endpoint.getEndpointUri();
        Producer answer = producers.get(key);


        if (answer == null) {
            // create a new producer
            try {
                answer = endpoint.createProducer();
            } catch (Exception e) {
                throw new FailedToCreateProducerException(endpoint, e);
            }

            if (answer.isSingleton()) {
                LOG.debug("Adding to producer cache with key: {} for producer: {}", endpoint, answer);
                producers.put(key, answer);
            }
        }

        return answer;
    }


    /**
     * Returns the current size of the cache
     *
     * @return the current size
     */
    public int size() {
        int size = producers.size();

        LOG.trace("size = {}", size);
        return size;
    }

    /**
     * Gets the maximum cache size (capacity).
     * <p/>
     * Will return <tt>-1</tt> if it cannot determine this if a custom cache was used.
     *
     * @return the capacity
     */
    public int getCapacity() {
        int capacity = -1;
        if (producers instanceof LRUCache) {
            LRUCache<String, Producer> cache = (LRUCache<String, Producer>)producers;
            capacity = cache.getMaxCacheSize();
        }
        return capacity;
    }

    /**
     * Gets the cache hits statistic
     * <p/>
     * Will return <tt>-1</tt> if it cannot determine this if a custom cache was used.
     *
     * @return the hits
     */
    public long getHits() {
        long hits = -1;
        if (producers instanceof LRUCache) {
            LRUCache<String, Producer> cache = (LRUCache<String, Producer>)producers;
            hits = cache.getHits();
        }
        return hits;
    }

    /**
     * Gets the cache misses statistic
     * <p/>
     * Will return <tt>-1</tt> if it cannot determine this if a custom cache was used.
     *
     * @return the misses
     */
    public long getMisses() {
        long misses = -1;
        if (producers instanceof LRUCache) {
            LRUCache<String, Producer> cache = (LRUCache<String, Producer>)producers;
            misses = cache.getMisses();
        }
        return misses;
    }

    /**
     * Gets the cache evicted statistic
     * <p/>
     * Will return <tt>-1</tt> if it cannot determine this if a custom cache was used.
     *
     * @return the evicted
     */
    public long getEvicted() {
        long evicted = -1;
        if (producers instanceof LRUCache) {
            LRUCache<String, Producer> cache = (LRUCache<String, Producer>)producers;
            evicted = cache.getEvicted();
        }
        return evicted;
    }

    /**
     * Resets the cache statistics
     */
    public void resetCacheStatistics() {
        if (producers instanceof LRUCache) {
            LRUCache<String, Producer> cache = (LRUCache<String, Producer>)producers;
            cache.resetStatistics();
        }
    }

    /**
     * Purges this cache
     */
    public synchronized void purge() {
        producers.clear();

    }

    @Override
    public String toString() {
        return "ProducerCache for source: " + source + ", capacity: " + getCapacity();
    }

    /**
     * Sends the exchange to the given endpoint.
     * <p>
     * This method will <b>not</b> throw an exception. If processing of the given
     * Exchange failed then the exception is stored on the provided Exchange
     *
     * @param endpoint the endpoint to send the exchange to
     * @param exchange the exchange to send
     */
    public void send(Endpoint endpoint, Exchange exchange) {
        sendExchange(endpoint, null, null, exchange);
    }

    /**
     * Sends an exchange to an endpoint using a supplied
     * {@link com.nxttxn.vramel.AsyncProcessor} to populate the exchange
     * <p>
     * This method will <b>not</b> throw an exception. If processing of the given
     * Exchange failed then the exception is stored on the return Exchange
     *
     * @param endpoint the endpoint to send the exchange to
     * @param processor the transformer used to populate the new exchange
     * @throws org.apache.camel.CamelExecutionException is thrown if sending failed
     * @return the exchange
     */
    public Exchange send(Endpoint endpoint, Processor processor) {
        return sendExchange(endpoint, null, processor, null);
    }

    /**
     * Sends an exchange to an endpoint using a supplied
     * {@link com.nxttxn.vramel.AsyncProcessor} to populate the exchange
     * <p>
     * This method will <b>not</b> throw an exception. If processing of the given
     * Exchange failed then the exception is stored on the return Exchange
     *
     * @param endpoint the endpoint to send the exchange to
     * @param pattern the message {@link ExchangePattern} such as
     *   {@link ExchangePattern#InOnly} or {@link ExchangePattern#InOut}
     * @param processor the transformer used to populate the new exchange
     * @return the exchange
     */
    public Exchange send(Endpoint endpoint, ExchangePattern pattern, Processor processor) {
        return sendExchange(endpoint, pattern, processor, null);
    }

    protected Exchange sendExchange(final Endpoint endpoint, ExchangePattern pattern,
                                    final Processor processor, Exchange exchange) {
        throw new UnsupportedOperationException("Not functional right now. Come implement this method");
    }
}
