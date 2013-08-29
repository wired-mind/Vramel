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


import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * A processor that processes the processor in a {@link org.apache.camel.spi.SubUnitOfWork} context.
 * <p/>
 * This processor ensures the {@link org.apache.camel.spi.UnitOfWork#beginSubUnitOfWork(org.apache.camel.Exchange)}
 * and {@link org.apache.camel.spi.UnitOfWork#endSubUnitOfWork(org.apache.camel.Exchange)} is executed.
 *
 * @see org.apache.camel.spi.SubUnitOfWork
 * @see org.apache.camel.spi.SubUnitOfWorkCallback
 */
public class SubUnitOfWorkProcessor extends UnitOfWorkProcessor {

    // See code comment in DefaultUnitOfWork for reasons why this implementation is named SubUnitOfWorkProcessor

    public SubUnitOfWorkProcessor(Processor processor) {
        super(processor);
    }

    public SubUnitOfWorkProcessor(AsyncProcessor processor) {
        super(processor);
    }

    public SubUnitOfWorkProcessor(FlowContext flowContext, Processor processor) {
        super(flowContext, processor);
    }

    public SubUnitOfWorkProcessor(FlowContext flowContext, AsyncProcessor processor) {
        super(flowContext, processor);
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        // begin savepoint
        exchange.getUnitOfWork().beginSubUnitOfWork(exchange);
        // process the exchange
        return super.process(exchange, new OptionalAsyncResultHandler() {
            @Override
            public void handle(AsyncExchangeResult event) {
                try {
                    // end sub unit of work
                    exchange.getUnitOfWork().endSubUnitOfWork(exchange);
                } finally {
                    // must ensure callback is invoked
                    optionalAsyncResultHandler.done(exchange);
                }
            }
        });
    }

    @Override
    public String toString() {
        return "SubUnitOfWorkProcessor[" + processor + "]";
    }
}
