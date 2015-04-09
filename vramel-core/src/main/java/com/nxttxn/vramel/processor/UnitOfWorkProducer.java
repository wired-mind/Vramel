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
package com.nxttxn.vramel.processor;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.util.ServiceHelper;
import org.vertx.java.core.AsyncResultHandler;

/**
 * Ensures a {@link Producer} is executed within an {@link org.apache.camel.spi.UnitOfWork}.
 *
 * @version
 */
public final class UnitOfWorkProducer implements Producer {

    private final Producer producer;
    private final Processor processor;

    /**
     * The producer which should be executed within an {@link org.apache.camel.spi.UnitOfWork}.
     *
     * @param producer the producer
     */
    public UnitOfWorkProducer(Producer producer) {
        this.producer = producer;
        this.processor = new UnitOfWorkProcessor(producer);
    }

    public Endpoint getEndpoint() {
        return producer.getEndpoint();
    }

    public Exchange createExchange() {
        return producer.createExchange();
    }

    public Exchange createExchange(ExchangePattern pattern) {
        return producer.createExchange(pattern);
    }

    public Exchange createExchange(Exchange exchange) {
        return producer.createExchange(exchange);
    }


    public void process(Exchange exchange) throws Exception {
        processor.process(exchange);
    }



    public void start(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        ServiceHelper.startService(processor);
    }

    public void stop() throws Exception {
        ServiceHelper.stopService(processor);
    }

    public boolean isSingleton() {
        return producer.isSingleton();
    }

    @Override
    public String toString() {
        return "UnitOfWork(" + producer + ")";
    }
}
