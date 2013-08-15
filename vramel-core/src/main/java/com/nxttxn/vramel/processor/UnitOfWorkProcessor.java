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

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.impl.DefaultUnitOfWork;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.spi.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures the {@link Exchange} is routed under the boundaries of an {@link org.apache.camel.spi.UnitOfWork}.
 * <p/>
 * Handles calling the {@link org.apache.camel.spi.UnitOfWork#done(org.apache.camel.Exchange)} method
 * when processing of an {@link Exchange} is complete.
 */
public class UnitOfWorkProcessor extends DelegateAsyncProcessor {

    private static final transient Logger LOG = LoggerFactory.getLogger(UnitOfWorkProcessor.class);
    private final FlowContext flowContext;
    private final String flowId;


    public UnitOfWorkProcessor(Processor processor) {
        this(null, processor);
    }

    public UnitOfWorkProcessor(AsyncProcessor processor) {
        this(null, processor);
    }

    public UnitOfWorkProcessor(FlowContext flowContext, Processor processor) {
        super(processor);
        this.flowContext = flowContext;
        if (flowContext != null) {
            this.flowId = flowContext.getFlow().idOrCreate(flowContext.getVramelContext().getNodeIdFactory());
        } else {
            this.flowId = null;
        }
    }

    public UnitOfWorkProcessor(FlowContext flowContext, AsyncProcessor processor) {
        super(processor);
        this.flowContext = flowContext;
        if (flowContext != null) {
            this.flowId = flowContext.getFlow().idOrCreate(flowContext.getVramelContext().getNodeIdFactory());
        } else {
            this.flowId = null;
        }
    }

    @Override
    public String toString() {
        return "UnitOfWork(" + processor + ")";
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        // if the exchange doesn't have from route id set, then set it if it originated
        // from this unit of work
        if (flowId != null && exchange.getFromRouteId() == null) {
            exchange.setFromRouteId(flowId);
        }

        if (exchange.getUnitOfWork() == null) {
            // If there is no existing UoW, then we should start one and
            // terminate it once processing is completed for the exchange.
            final UnitOfWork uow = createUnitOfWork(exchange);
            exchange.setUnitOfWork(uow);

            return getProcessor().process(exchange, new OptionalAsyncResultHandler() {
                @Override
                public void handle(AsyncExchangeResult optionalAsyncResult) {
                    final Exchange result = optionalAsyncResult.result.get();
                    optionalAsyncResultHandler.done(result);
                    result.setUnitOfWork(null);
                }
            });

        } else {
            // There was an existing UoW, so we should just pass through..
            // so that the guy the initiated the UoW can terminate it.
            super.process(exchange, optionalAsyncResultHandler);
            return false;
        }
    }


    /**
     * Strategy to create the unit of work for the given exchange.
     *
     * @param exchange the exchange
     * @return the created unit of work
     */
    protected UnitOfWork createUnitOfWork(Exchange exchange) {

        return new DefaultUnitOfWork(exchange);

    }



}
