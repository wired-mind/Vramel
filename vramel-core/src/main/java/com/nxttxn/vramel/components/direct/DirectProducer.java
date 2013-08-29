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
package com.nxttxn.vramel.components.direct;


import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The direct producer.
 *
 * @version 
 */
public class DirectProducer extends DefaultAsyncProducer {
    private static final transient Logger LOG = LoggerFactory.getLogger(DirectProducer.class);
    private final DirectEndpoint endpoint;

    public DirectProducer(DirectEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    public void process(Exchange exchange) throws Exception {
        if (endpoint.getConsumer() == null) {
            LOG.warn("No consumers available on endpoint: " + endpoint + " to process: " + exchange);
            throw new DirectConsumerNotAvailableException("No consumers available on endpoint: " + endpoint, exchange);
        } else {
            endpoint.getConsumer().getProcessor().process(exchange);
        }
    }

    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        if (endpoint.getConsumer() == null) {
            LOG.warn("No consumers available on endpoint: " + endpoint + " to process: " + exchange);
            // indicate its done synchronously
            exchange.setException(new DirectConsumerNotAvailableException("No consumers available on endpoint: " + endpoint, exchange));
            optionalAsyncResultHandler.done(exchange);
            return true;
        } else {
            AsyncProcessor processor = AsyncProcessorConverterHelper.convert(endpoint.getConsumer().getProcessor());

            return processor.process(exchange, optionalAsyncResultHandler);

        }
    }

}
