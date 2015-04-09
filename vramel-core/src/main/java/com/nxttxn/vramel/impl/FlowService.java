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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.OnCompletionDefinition;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.model.ProcessorDefinition;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.support.ChildServiceSupport;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;

/**
 * Represents the runtime objects for a given {@link FlowDefinition} so that it can be stopped independently
 * of other flows
 *
 * @version
 */
public class FlowService extends ChildServiceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(FlowService.class);

    private final DefaultVramelContext vramelContext;
    private final FlowDefinition flowDefinition;
    private final List<FlowContext> flowContexts;
    private final List<Flow> flows;
    private final String id;
    private boolean removingRoutes;
    private final Map<Flow, Consumer> inputs = new HashMap<Flow, Consumer>();
    private final AtomicBoolean warmUpDone = new AtomicBoolean(false);
    private final AtomicBoolean endpointDone = new AtomicBoolean(false);

    public FlowService(DefaultVramelContext vramelContext, FlowDefinition flowDefinition, List<FlowContext> flowContexts, List<Flow> flows) {
        this.vramelContext = vramelContext;
        this.flowDefinition = flowDefinition;
        this.flowContexts = flowContexts;
        this.flows = flows;
        this.id = flowDefinition.idOrCreate(vramelContext.getNodeIdFactory());
    }

    public String getId() {
        return id;
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public List<FlowContext> getFlowContexts() {
        return flowContexts;
    }

    public FlowDefinition getFlowDefinition() {
        return flowDefinition;
    }

    public Collection<Flow> getFlows() {
        return flows;
    }

    /**
     * Gets the inputs to the flows.
     *
     * @return list of {@link Consumer} as inputs for the flows
     */
    public Map<Flow, Consumer> getInputs() {
        return inputs;
    }

    public boolean isRemovingRoutes() {
        return removingRoutes;
    }

    public void setRemovingRoutes(boolean removingRoutes) {
        this.removingRoutes = removingRoutes;
    }

    public synchronized void warmUp() throws Exception {
        if (endpointDone.compareAndSet(false, true)) {
            // endpoints should only be started once as they can be reused on other flows
            // and whatnot, thus their lifecycle is to start once, and only to stop when Camel shutdown
            for (Flow flow : flows) {
                // ensure endpoint is started first (before the flow services, such as the consumer)
                ServiceHelper.startService(flow.getEndpoint());
            }
        }

        if (warmUpDone.compareAndSet(false, true)) {

            for (Flow flow : flows) {
                // warm up the flow first
                flow.warmUp();

                LOG.debug("Starting services on flow: {}", flow.getId());
                List<Service> services = flow.getServices();

                // callback that we are staring these services
                flow.onStartingServices(services);

                // gather list of services to start as we need to start child services as well
                Set<Service> list = new LinkedHashSet<Service>();
                for (Service service : services) {
                    list.addAll(ServiceHelper.getChildServices(service));
                }

                // split into consumers and child services as we need to start the consumers
                // afterwards to avoid them being active while the others start
                List<Service> childServices = new ArrayList<Service>();
                for (Service service : list) {
                    if (service instanceof Consumer) {
                        inputs.put(flow, (Consumer) service);
                    } else {
                        childServices.add(service);
                    }
                }
                startChildService(flow, childServices);
            }

//            // ensure lifecycle strategy is invoked which among others enlist the route in JMX
//            for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//                strategy.onRoutesAdd(flows);
//            }

            // add flows to camel context
            vramelContext.addFlowCollection(flows);
        }
    }

    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        // ensure we are warmed up before starting the route
        warmUp();

        for (Flow route : flows) {
            // start the route itself
            ServiceHelper.startService(route);

//            // invoke callbacks on route policy
//            if (route.getFlowContext().getRoutePolicyList() != null) {
//                for (RoutePolicy routePolicy : route.getFlowContext().getRoutePolicyList()) {
//                    routePolicy.onStart(route);
//                }
//            }
//
//            // fire event
//            EventHelper.notifyRouteStarted(vramelContext, route);
        }
    }

    protected void doStop() throws Exception {

        // if we are stopping CamelContext then we are shutting down
        boolean isShutdownCamelContext = vramelContext.isStopping();

//        if (isShutdownCamelContext || isRemovingRoutes()) {
//            // need to call onRoutesRemove when the CamelContext is shutting down or Route is shutdown
//            for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//                strategy.onRoutesRemove(flows);
//            }
//        }

        for (Flow route : flows) {
            LOG.debug("Stopping services on route: {}", route.getId());

            // gather list of services to stop as we need to start child services as well
            List<Service> services = new ArrayList<Service>();
            services.addAll(route.getServices());
            // also get route scoped services
            doGetFlowScopedServices(services, route);
            Set<Service> list = new LinkedHashSet<Service>();
            for (Service service : services) {
                list.addAll(ServiceHelper.getChildServices(service));
            }
            // also get route scoped error handler (which must be done last)
            doGetFlowScopedErrorHandler(list, route);

            // stop services
            stopChildService(route, list, isShutdownCamelContext);

            // stop the route itself
            if (isShutdownCamelContext) {
                ServiceHelper.stopAndShutdownServices(route);
            } else {
                ServiceHelper.stopServices(route);
            }

//            // invoke callbacks on route policy
//            if (route.getFlowContext().getRoutePolicyList() != null) {
//                for (RoutePolicy routePolicy : route.getFlowContext().getRoutePolicyList()) {
//                    routePolicy.onStop(route);
//                }
//            }
//            // fire event
//            EventHelper.notifyRouteStopped(vramelContext, route);
        }
        if (isRemovingRoutes()) {
            vramelContext.removeFlowCollection(flows);
        }
        // need to warm up again
        warmUpDone.set(false);
    }

    @Override
    protected void doShutdown() throws Exception {
        for (Flow route : flows) {
            LOG.debug("Shutting down services on route: {}", route.getId());

            // gather list of services to stop as we need to start child services as well
            List<Service> services = new ArrayList<Service>();
            services.addAll(route.getServices());
            // also get route scoped services
            doGetFlowScopedServices(services, route);
            Set<Service> list = new LinkedHashSet<Service>();
            for (Service service : services) {
                list.addAll(ServiceHelper.getChildServices(service));
            }
            // also get route scoped error handler (which must be done last)
            doGetFlowScopedErrorHandler(list, route);

            // shutdown services
            stopChildService(route, list, true);

            // shutdown the route itself
            ServiceHelper.stopAndShutdownServices(route);

            // endpoints should only be stopped when Camel is shutting down
            // see more details in the warmUp method
            ServiceHelper.stopAndShutdownServices(route.getEndpoint());
//            // invoke callbacks on route policy
//            if (route.getFlowContext().getRoutePolicyList() != null) {
//                for (RoutePolicy routePolicy : route.getRouteContext().getRoutePolicyList()) {
//                    routePolicy.onRemove(route);
//                }
//            }
        }

//        // need to call onRoutesRemove when the CamelContext is shutting down or Route is shutdown
//        for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//            strategy.onRoutesRemove(flows);
//        }
//
//        // remove the flows from the inflight registry
//        for (Flow route : flows) {
//            vramelContext.getInflightRepository().removeRoute(route.getId());
//        }

        // remove the flows from the collections
        vramelContext.removeFlowCollection(flows);

        // clear inputs on shutdown
        inputs.clear();
        warmUpDone.set(false);
        endpointDone.set(false);
    }

    @Override
    protected void doSuspend() throws Exception {
        // suspend and resume logic is provided by DefaultCamelContext which leverages ShutdownStrategy
        // to safely suspend and resume
//        for (Flow route : flows) {
//            if (route.getFlowContext().getRoutePolicyList() != null) {
//                for (RoutePolicy routePolicy : route.getFlowContext().getRoutePolicyList()) {
//                    routePolicy.onSuspend(route);
//                }
//            }
//        }
    }

    @Override
    protected void doResume() throws Exception {
        // suspend and resume logic is provided by DefaultCamelContext which leverages ShutdownStrategy
        // to safely suspend and resume
//        for (Flow route : flows) {
//            if (route.getFlowContext().getRoutePolicyList() != null) {
//                for (RoutePolicy routePolicy : route.getFlowContext().getRoutePolicyList()) {
//                    routePolicy.onResume(route);
//                }
//            }
//        }
    }

    protected void startChildService(Flow route, List<Service> services) throws Exception {
        for (Service service : services) {
            LOG.debug("Starting child service on route: {} -> {}", route.getId(), service);
//            for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//                strategy.onServiceAdd(vramelContext, service, route);
//            }
            ServiceHelper.startService(service);
            addChildService(service);
        }
    }

    protected void stopChildService(Flow route, Set<Service> services, boolean shutdown) throws Exception {
        for (Service service : services) {
            LOG.debug("{} child service on route: {} -> {}", new Object[]{shutdown ? "Shutting down" : "Stopping", route.getId(), service});
//            if (service instanceof ErrorHandler) {
//                // special for error handlers
//                for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//                    strategy.onErrorHandlerRemove(route.getFlowContext(), (Processor) service, route.getFlowContext().getFlow().getErrorHandlerBuilder());
//                }
//            } else {
//                for (LifecycleStrategy strategy : vramelContext.getLifecycleStrategies()) {
//                    strategy.onServiceRemove(vramelContext, service, route);
//                }
//            }
            if (shutdown) {
                ServiceHelper.stopAndShutdownService(service);
            } else {
                ServiceHelper.stopService(service);
            }
            removeChildService(service);
        }
    }

    /**
     * Gather the route scoped error handler from the given route
     */
    private void doGetFlowScopedErrorHandler(Set<Service> services, Flow route) {
        // only include error handlers if they are route scoped
        boolean includeErrorHandler = !flowDefinition.isContextScopedErrorHandler(route.getFlowContext().getVramelContext());
        List<Service> extra = new ArrayList<Service>();
        if (includeErrorHandler) {
            for (Service service : services) {
                if (service instanceof Channel) {
                    Processor eh = ((Channel) service).getErrorHandler();
                    if (eh != null && eh instanceof Service) {
                        extra.add((Service) eh);
                    }
                }
            }
        }
        if (!extra.isEmpty()) {
            services.addAll(extra);
        }
    }

    /**
     * Gather all other kind of route scoped services from the given route, except error handler
     */
    private void doGetFlowScopedServices(List<Service> services, Flow route) {
        for (ProcessorDefinition<?> output : route.getFlowContext().getFlow().getOutputs()) {
            if (output instanceof OnExceptionDefinition) {
                OnExceptionDefinition onExceptionDefinition = (OnExceptionDefinition) output;
                if (onExceptionDefinition.isFlowScoped()) {
                    Processor errorHandler = onExceptionDefinition.getErrorHandler(route.getId());
                    if (errorHandler != null && errorHandler instanceof Service) {
                        services.add((Service) errorHandler);
                    }
                }
            } else if (output instanceof OnCompletionDefinition) {
                OnCompletionDefinition onCompletionDefinition = (OnCompletionDefinition) output;
                if (onCompletionDefinition.getFlowScoped()) {
                    Processor onCompletionProcessor = onCompletionDefinition.getOnCompletion(route.getId());
                    if (onCompletionProcessor != null && onCompletionProcessor instanceof Service) {
                        services.add((Service) onCompletionProcessor);
                    }
                }
            }
        }
    }

}
