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
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.spi.UnitOfWork;

/**
 * This processor tracks the current {@link FlowContext} while processing the {@link Exchange}.
 * This ensures that the {@link Exchange} have details under which route its being currently processed.
 */
public class FlowContextProcessor extends DelegateAsyncProcessor {

    private final FlowContext flowContext;

    public FlowContextProcessor(FlowContext flowContext, Processor processor) {
        super(processor);
        this.flowContext = flowContext;
    }

    @Override
    protected boolean processNext(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        // push the current route context
        final UnitOfWork unitOfWork = exchange.getUnitOfWork();
        if (unitOfWork != null) {
            unitOfWork.pushFlowContext(flowContext);
        }

        return processor.process(exchange, new OptionalAsyncResultHandler() {
            @Override
            public void handle(AsyncExchangeResult optionalAsyncResult) {
                try {
                    // pop the route context we just used
                    if (unitOfWork != null) {
                        unitOfWork.popFlowContext();
                    }
                } catch (Exception e) {
                    exchange.setException(e);
                } finally {
                    optionalAsyncResultHandler.done(optionalAsyncResult.result.or(exchange));
                }
            }
        });
    }

    @Override
    public String toString() {
        return "FlowContextProcessor[" + processor + "]";
    }

}
