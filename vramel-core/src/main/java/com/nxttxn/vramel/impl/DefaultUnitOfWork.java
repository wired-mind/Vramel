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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;


import com.nxttxn.vramel.*;
import com.nxttxn.vramel.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link org.apache.camel.spi.UnitOfWork}
 */
public class DefaultUnitOfWork implements UnitOfWork, Service {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUnitOfWork.class);

    // TODO: This implementation seems to have transformed itself into a to broad concern
    // where unit of work is doing a bit more work than the transactional aspect that ties
    // to its name. Maybe this implementation should be named ExchangeContext and we can
    // introduce a simpler UnitOfWork concept. This would also allow us to refactor the
    // SubUnitOfWork into a general parent/child unit of work concept. However this
    // requires API changes and thus is best kept for Camel 3.0

    private UnitOfWork parent;
    private String id;
    private VramelContext context;
    private List<Synchronization> synchronizations;
    private Message originalInMessage;
    private final Stack<FlowContext> routeContextStack = new Stack<FlowContext>();
    private Stack<DefaultSubUnitOfWork> subUnitOfWorks;

    private final transient Logger log;

    public DefaultUnitOfWork(Exchange exchange) {
        this(exchange, LOG);
    }

    protected DefaultUnitOfWork(Exchange exchange, Logger logger) {
        log = logger;
        if (log.isTraceEnabled()) {
            log.trace("UnitOfWork created for ExchangeId: {} with {}", exchange.getExchangeId(), exchange);
        }

        context = exchange.getContext();

        // TODO: Camel 3.0: the copy on facade strategy will help us here in the future
        // TODO: optimize to only copy original message if enabled to do so in the route
        // special for JmsMessage as it can cause it to loose headers later.
        // This will be resolved when we get the message facade with copy on write implemented
        if (exchange.getIn().getClass().getSimpleName().equals("JmsMessage")) {
            this.originalInMessage = new DefaultMessage();
            this.originalInMessage.setBody(exchange.getIn().getBody());
            this.originalInMessage.setHeaders(exchange.getIn().getHeaders());
        } else {
            this.originalInMessage = exchange.getIn().copy();
        }

        // TODO: Optimize to only copy if useOriginalMessage has been enabled

        // mark the creation time when this Exchange was created
        if (exchange.getProperty(Exchange.CREATED_TIMESTAMP) == null) {
            exchange.setProperty(Exchange.CREATED_TIMESTAMP, new Date());
        }


    }

    UnitOfWork newInstance(Exchange exchange) {
        return new DefaultUnitOfWork(exchange);
    }




    public String getId() {
        if (id == null) {
            id = context.getUuidGenerator().generateUuid();
        }
        return id;
    }

    public Message getOriginalInMessage() {
        return originalInMessage;
    }


    public FlowContext getFlowContext() {
        synchronized (routeContextStack) {
            if (routeContextStack.isEmpty()) {
                return null;
            }
            return routeContextStack.peek();
        }
    }

    public void pushFlowContext(FlowContext flowContext) {
        synchronized (routeContextStack) {
            routeContextStack.add(flowContext);
        }
    }

    public FlowContext popFlowContext() {
        synchronized (routeContextStack) {
            if (routeContextStack.isEmpty()) {
                return null;
            }
            return routeContextStack.pop();
        }
    }


    public void start() throws Exception {
        id = null;
    }

    public void stop() throws Exception {
        // need to clean up when we are stopping to not leak memory
        //for now comment out features not ported yet from camel
        if (synchronizations != null) {
            synchronizations.clear();
        }
//        if (tracedRouteNodes != null) {
//            tracedRouteNodes.clear();
//        }
//        if (transactedBy != null) {
//            transactedBy.clear();
//        }
        synchronized (routeContextStack) {
            if (!routeContextStack.isEmpty()) {
                routeContextStack.clear();
            }
        }
//        if (subUnitOfWorks != null) {
//            subUnitOfWorks.clear();
//        }
        originalInMessage = null;
        parent = null;
        id = null;
    }

    @Override
    public String toString() {
        return "DefaultUnitOfWork";
    }

    public synchronized void addSynchronization(Synchronization synchronization) {
        if (synchronizations == null) {
            synchronizations = new ArrayList<Synchronization>();
        }
        log.trace("Adding synchronization {}", synchronization);
        synchronizations.add(synchronization);
    }

    public synchronized void removeSynchronization(Synchronization synchronization) {
        if (synchronizations != null) {
            synchronizations.remove(synchronization);
        }
    }

    public synchronized boolean containsSynchronization(Synchronization synchronization) {
        return synchronizations != null && synchronizations.contains(synchronization);
    }

    public void handoverSynchronization(Exchange target) {
        if (synchronizations == null || synchronizations.isEmpty()) {
            return;
        }

        Iterator<Synchronization> it = synchronizations.iterator();
        while (it.hasNext()) {
            Synchronization synchronization = it.next();

            boolean handover = true;
            if (synchronization instanceof SynchronizationVetoable) {
                SynchronizationVetoable veto = (SynchronizationVetoable) synchronization;
                handover = veto.allowHandover();
            }

            if (handover) {
                log.trace("Handover synchronization {} to: {}", synchronization, target);
                target.addOnCompletion(synchronization);
                // remove it if its handed over
                it.remove();
            } else {
                log.trace("Handover not allow for synchronization {}", synchronization);
            }
        }
    }

    public UnitOfWork createChildUnitOfWork(Exchange childExchange) {
        // create a new child unit of work, and mark me as its parent
        UnitOfWork answer = newInstance(childExchange);
        answer.setParentUnitOfWork(this);
        return answer;
    }

    @Override
    public void setParentUnitOfWork(UnitOfWork parentUnitOfWork) {
        this.parent = parentUnitOfWork;
    }

    @Override
    public void beginSubUnitOfWork(Exchange exchange) {
        if (log.isTraceEnabled()) {
            log.trace("beginSubUnitOfWork exchangeId: {}", exchange.getExchangeId());
        }

        if (subUnitOfWorks == null) {
            subUnitOfWorks = new Stack<DefaultSubUnitOfWork>();
        }
        subUnitOfWorks.push(new DefaultSubUnitOfWork());
    }

    @Override
    public void endSubUnitOfWork(Exchange exchange) {
        if (log.isTraceEnabled()) {
            log.trace("endSubUnitOfWork exchangeId: {}", exchange.getExchangeId());
        }

        if (subUnitOfWorks == null || subUnitOfWorks.isEmpty()) {
            return;
        }

        // pop last sub unit of work as its now ended
        SubUnitOfWork subUoW = subUnitOfWorks.pop();
        if (subUoW.isFailed()) {
            // the sub unit of work failed so set an exception containing all the caused exceptions
            // and mark the exchange for rollback only

            // if there are multiple exceptions then wrap those into another exception with them all
            Exception cause;
            List<Exception> list = subUoW.getExceptions();
            if (list != null) {
                if (list.size() == 1) {
                    cause = list.get(0);
                } else {
                    cause = new VramelUnitOfWorkException(exchange, list);
                }
                exchange.setException(cause);
            }
            // mark it as rollback and that the unit of work is exhausted. This ensures that we do not try
            // to redeliver this exception (again)
            exchange.setProperty(Exchange.ROLLBACK_ONLY, true);
            exchange.setProperty(Exchange.UNIT_OF_WORK_EXHAUSTED, true);
            // and remove any indications of error handled which will prevent this exception to be noticed
            // by the error handler which we want to react with the result of the sub unit of work
            exchange.setProperty(Exchange.ERRORHANDLER_HANDLED, null);
            exchange.setProperty(Exchange.FAILURE_HANDLED, null);
            if (log.isTraceEnabled()) {
                log.trace("endSubUnitOfWork exchangeId: {} with {} caused exceptions.", exchange.getExchangeId(), list != null ? list.size() : 0);
            }
        }
    }
}
