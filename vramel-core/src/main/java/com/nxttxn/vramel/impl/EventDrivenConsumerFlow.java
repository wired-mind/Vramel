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
import com.nxttxn.vramel.processor.DelegateProcessor;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.List;


/**
 * A {@link DefaultFlow} which starts with an
 * <a href="http://camel.apache.org/event-driven-consumer.html">Event Driven Consumer</a>
 * <p/>
 * Use the API from {@link org.apache.camel.CamelContext} to control the lifecycle of a route,
 * such as starting and stopping using the {@link org.apache.camel.CamelContext#startRoute(String)}
 * and {@link org.apache.camel.CamelContext#stopRoute(String)} methods.
 *
 * @version
 */
public class EventDrivenConsumerFlow extends DefaultFlow {
    private final Processor processor;
    private Consumer consumer;

    public EventDrivenConsumerFlow(FlowContext flowContext, Endpoint endpoint, Processor processor) {
        super(flowContext, endpoint);
        this.processor = processor;
    }

    @Override
    public String toString() {
        return "EventDrivenConsumerRoute[" + getEndpoint() + " -> " + processor + "]";
    }

    public Processor getProcessor() {
        return processor;
    }

    /**
     * Factory method to lazily create the complete list of services required for this route
     * such as adding the processor or consumer
     */
    @Override
    protected void addServices(List<Service> services) throws Exception {
        Endpoint endpoint = getEndpoint();
        consumer = endpoint.createConsumer(processor);
        if (consumer != null) {
            services.add(consumer);
        }
        Processor processor = getProcessor();
        if (processor instanceof Service) {
            services.add((Service)processor);
        }
    }

    @SuppressWarnings("unchecked")
    public Navigate<Processor> navigate() {
        Processor answer = getProcessor();

        // we do not want to navigate the instrument and inflight processors
        // which is the first 2 delegate async processors, so skip them
        // skip the instrumentation processor if this route was wrapped by one
        if (answer instanceof DelegateProcessor) {
            answer = ((DelegateProcessor) answer).getProcessor();
            if (answer instanceof DelegateProcessor) {
                answer = ((DelegateProcessor) answer).getProcessor();
            }
        }

        if (answer instanceof Navigate) {
            return (Navigate<Processor>) answer;
        }
        return null;
    }

    public Consumer getConsumer() {
        return consumer;
    }


}