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
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.spi.UnitOfWork;

/**
 * An {@link UnitOfWorkProcessor} that creates a child {@link UnitOfWork} that is
 * associated to a parent {@link UnitOfWork}.
 *
 * @see SubUnitOfWorkProcessor
 */
public class ChildUnitOfWorkProcessor extends UnitOfWorkProcessor {

    private final UnitOfWork parent;

    public ChildUnitOfWorkProcessor(UnitOfWork parent, Processor processor) {
        super(processor);
        this.parent = parent;
    }

    public ChildUnitOfWorkProcessor(UnitOfWork parent, AsyncProcessor processor) {
        super(processor);
        this.parent = parent;
    }

    public ChildUnitOfWorkProcessor(UnitOfWork parent, FlowContext flowContext, Processor processor) {
        super(flowContext, processor);
        this.parent = parent;
    }

    public ChildUnitOfWorkProcessor(UnitOfWork parent, FlowContext flowContext, AsyncProcessor processor) {
        super(flowContext, processor);
        this.parent = parent;
    }

    @Override
    protected UnitOfWork createUnitOfWork(Exchange exchange) {
        // let the parent create a child unit of work to be used
        return parent.createChildUnitOfWork(exchange);
    }

}
