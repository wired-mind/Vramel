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

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Flow;
import com.nxttxn.vramel.Service;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of {@link Flow}.
 * <p/>
 * Use the API from {@link org.apache.camel.CamelContext} to control the lifecycle of a route,
 * such as starting and stopping using the {@link org.apache.camel.CamelContext#startRoute(String)}
 * and {@link org.apache.camel.CamelContext#stopRoute(String)} methods.
 *
 * @version
 */
public abstract class DefaultFlow implements Flow {

    private final Endpoint endpoint;
    private final Map<String, Object> properties = new HashMap<String, Object>();
    private final List<Service> services = new ArrayList<Service>();
    private final FlowContext flowContext;

    public DefaultFlow(FlowContext flowContext, Endpoint endpoint) {
        this.flowContext = flowContext;
        this.endpoint = endpoint;
    }

    public DefaultFlow(FlowContext flowContext, Endpoint endpoint, Service... services) {
        this(flowContext, endpoint);
        for (Service service : services) {
            addService(service);
        }
    }

    @Override
    public String toString() {
        return "Route " + getId();
    }

    public String getId() {
        return (String) properties.get(Flow.ID_PROPERTY);
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void onStartingServices(List<Service> services) throws Exception {
        addServices(services);
    }

    @Override
    public List<Service> getServices() {
        return services;
    }

    public void addService(Service service) {
        if (!services.contains(service)) {
            services.add(service);
        }
    }

    public void warmUp() {
        getServices().clear();
    }



    /**
     * Strategy method to allow derived classes to lazily load services for the route
     */
    protected void addServices(List<Service> services) throws Exception {
    }

    protected void doStart() throws Exception {
        // noop
    }

    protected void doStop() throws Exception {
        // noop
    }


}
