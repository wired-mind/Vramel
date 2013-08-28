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

import com.nxttxn.vramel.Consumer;
import com.nxttxn.vramel.Flow;
import com.nxttxn.vramel.Service;
import com.nxttxn.vramel.spi.RouteStartupOrder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of {@link org.apache.camel.spi.RouteStartupOrder}.
 *
 * @version
 */
public class DefaultFlowStartupOrder implements RouteStartupOrder {

    private final int startupOrder;
    private final Flow flow;
    private final FlowService flowService;

    public DefaultFlowStartupOrder(int startupOrder, Flow flow, FlowService flowService) {
        this.startupOrder = startupOrder;
        this.flow = flow;
        this.flowService = flowService;
    }

    public int getStartupOrder() {
        return startupOrder;
    }

    public Flow getFlow() {
        return flow;
    }

    public List<Consumer> getInputs() {
        List<Consumer> answer = new ArrayList<Consumer>();
        Map<Flow, Consumer> inputs = flowService.getInputs();
        for (Consumer consumer : inputs.values()) {
            answer.add(consumer);
        }
        return answer;
    }

    public List<Service> getServices() {
        List<Service> answer = new ArrayList<Service>();
        Collection<Flow> flows = flowService.getFlows();
        for (Flow flow : flows) {
            answer.addAll(flow.getServices());
        }
        return answer;
    }

    public FlowService getFlowService() {
        return flowService;
    }

    @Override
    public String toString() {
        return "Flow " + flow.getId() + " starts in order " + startupOrder;
    }
}
