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
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.FromDefinition;
import com.nxttxn.vramel.model.ProcessorDefinition;
import com.nxttxn.vramel.processor.PipelineProcessor;
import com.nxttxn.vramel.processor.UnitOfWorkProcessor;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.VramelContextHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * The context used to activate new routing rules
 *
 * @version
 */
public class DefaultFlowContext implements FlowContext {
    private final Map<ProcessorDefinition<?>, AtomicInteger> nodeIndex = new HashMap<ProcessorDefinition<?>, AtomicInteger>();
    private final FlowDefinition flow;
    private FromDefinition from;
    private final Collection<Flow> flows;
    private Endpoint endpoint;
    private VramelContext vramelContext;
    private final List<Processor> eventDrivenProcessors = new ArrayList<Processor>();
    private boolean flowAdded;

    private Boolean handleFault;


    public DefaultFlowContext(VramelContext vramelContext, FlowDefinition flow, FromDefinition from, Collection<Flow> flows) {
        this.vramelContext = vramelContext;
        this.flow = flow;
        this.from = from;
        this.flows = flows;
    }

    /**
     * Only used for lazy construction from inside ExpressionType
     */
    public DefaultFlowContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
        this.flows = new ArrayList<Flow>();
        this.flow = new FlowDefinition("temporary");
    }

    public Endpoint getEndpoint() {
        if (endpoint == null) {
            endpoint = from.resolveEndpoint(this);
        }
        return endpoint;
    }

    public FromDefinition getFrom() {
        return from;
    }

    public FlowDefinition getFlow() {
        return flow;
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public Endpoint resolveEndpoint(String uri) {
        return getVramelContext().getEndpoint(uri);
    }

    @Override
    public void addEventDrivenProcessor(Processor processor) {
        eventDrivenProcessors.add(processor);
    }


    public void setHandleFault(Boolean handleFault) {
        this.handleFault = handleFault;
    }

    public Boolean isHandleFault() {
        if (handleFault != null) {
            return handleFault;
        } else {
            // fallback to the option from camel context
            return getVramelContext().isHandleFault();
        }
    }


    public int getAndIncrement(ProcessorDefinition<?> node) {
        AtomicInteger count = nodeIndex.get(node);
        if (count == null) {
            count = new AtomicInteger();
            nodeIndex.put(node, count);
        }
        return count.getAndIncrement();
    }

    @Override
    public boolean isFlowAdded() {
        return flowAdded;
    }

    @Override
    public void setIsFlowAdded(boolean flowAdded) {
        this.flowAdded = flowAdded;
    }

    @Override
    public void commit() {
        // now lets turn all of the event driven consumer processors into a single route
        if (!eventDrivenProcessors.isEmpty()) {
            Processor target = new PipelineProcessor(eventDrivenProcessors);


            // and wrap it in a unit of work so the UoW is on the top, so the entire route will be in the same UoW
            UnitOfWorkProcessor unitOfWorkProcessor = new UnitOfWorkProcessor(this, target);
            target = unitOfWorkProcessor;


//            // wrap in route inflight processor to track number of inflight exchanges for the route
//            RouteInflightRepositoryProcessor inflight = new RouteInflightRepositoryProcessor(camelContext.getInflightRepository(), target);
//
//            // and wrap it by a instrumentation processor that is to be used for performance stats
//            // for this particular route
//            InstrumentationProcessor instrument = new InstrumentationProcessor();
//            instrument.setType("route");
//            instrument.setProcessor(inflight);

            // and create the route that wraps the UoW
            Flow edcf = new EventDrivenConsumerFlow(this, getEndpoint(), target);
//            // create the route id
            String routeId = flow.idOrCreate(getVramelContext().getNodeIdFactory());
            edcf.getProperties().put(Flow.ID_PROPERTY, routeId);
//            edcf.getProperties().put(Route.PARENT_PROPERTY, Integer.toHexString(route.hashCode()));
//            if (route.getGroup() != null) {
//                edcf.getProperties().put(Route.GROUP_PROPERTY, route.getGroup());
//            }
//
//            // after the route is created then set the route on the policy processor so we get hold of it
//            if (routePolicyProcessor != null) {
//                routePolicyProcessor.setRoute(edcf);
//            }
//            // after the route is created then set the route on the inflight processor so we get hold of it
//            inflight.setRoute(edcf);
//
//            // invoke init on route policy
//            if (routePolicyList != null && !routePolicyList.isEmpty()) {
//                for (RoutePolicy policy : routePolicyList) {
//                    policy.onInit(edcf);
//                }
//            }

            flows.add(edcf);
        }
    }

    @Override
    public <T> T mandatoryLookup(String name, Class<T> type) {
        return VramelContextHelper.mandatoryLookup(getVramelContext(), name, type);
    }
}
