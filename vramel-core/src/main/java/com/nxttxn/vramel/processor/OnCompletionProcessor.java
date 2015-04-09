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

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.support.SynchronizationAdapter;
import com.nxttxn.vramel.util.ExchangeHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;

import static com.nxttxn.vramel.util.ObjectHelper.notNull;

/**
 * @version
 */
public class OnCompletionProcessor extends ServiceSupport implements Processor {

    private static final transient Logger LOG = LoggerFactory.getLogger(OnCompletionProcessor.class);
    private final VramelContext vramelContext;
    private final Processor processor;
    private final boolean onCompleteOnly;
    private final boolean onFailureOnly;
    private final Predicate onWhen;
    private final boolean useOriginalBody;

    public OnCompletionProcessor(VramelContext vramelContext, Processor processor,
                                 boolean onCompleteOnly, boolean onFailureOnly, Predicate onWhen, boolean useOriginalBody) {
        notNull(vramelContext, "vramelContext");
        notNull(processor, "processor");
        this.vramelContext = vramelContext;
        // wrap processor in UnitOfWork so what we send out runs in a UoW
        this.processor = new UnitOfWorkProcessor(processor);

        this.onCompleteOnly = onCompleteOnly;
        this.onFailureOnly = onFailureOnly;
        this.onWhen = onWhen;
        this.useOriginalBody = useOriginalBody;
    }

    @Override
    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        ServiceHelper.startService(processor);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(processor);
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownService(processor);
//        if (shutdownExecutorService) {
//            getVramelContext().getExecutorServiceManager().shutdownNow(executorService);
//        }
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public void process(Exchange exchange) throws Exception {
        if (processor == null) {
            return;
        }

        // register callback
        exchange.getUnitOfWork().addSynchronization(new OnCompletionSynchronization());
    }

    /**
     * Processes the exchange by the processors
     *
     * @param processor the processor
     * @param exchange the exchange
     */
    protected static void doProcess(Processor processor, Exchange exchange) {
        try {
            processor.process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

    /**
     * Prepares the {@link Exchange} to send as onCompletion.
     *
     * @param exchange the current exchange
     * @return the exchange to be routed in onComplete
     */
    protected Exchange prepareExchange(Exchange exchange) {
        Exchange answer;

        // for asynchronous routing we must use a copy as we dont want it
        // to cause side effects of the original exchange
        // (the original thread will run in parallel)
        answer = ExchangeHelper.createCorrelatedCopy(exchange, false);
        if (answer.hasOut()) {
            // move OUT to IN (pipes and filters)
            answer.setIn(answer.getOut());
            answer.setOut(null);
        }
        // set MEP to InOnly as this wire tap is a fire and forget
        answer.setPattern(ExchangePattern.InOnly);

        if (useOriginalBody) {
            LOG.trace("Using the original IN message instead of current");

            Message original = exchange.getUnitOfWork().getOriginalInMessage();
            answer.setIn(original);
        }

        // add a header flag to indicate its a on completion exchange
        answer.setProperty(Exchange.ON_COMPLETION, Boolean.TRUE);

        return answer;
    }

    private final class OnCompletionSynchronization extends SynchronizationAdapter implements Ordered {

        public int getOrder() {
            // we want to be last
            return Ordered.LOWEST;
        }

        @Override
        public void onComplete(final Exchange exchange) {
            if (onFailureOnly) {
                return;
            }

            if (onWhen != null && !onWhen.matches(exchange)) {
                // predicate did not match so do not route the onComplete
                return;
            }

            // must use a copy as we dont want it to cause side effects of the original exchange
            final Exchange copy = prepareExchange(exchange);

            vramelContext.getVertx().runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    LOG.debug("Processing onComplete: {}", copy);
                    doProcess(processor, copy);
                }
            });

        }

        public void onFailure(final Exchange exchange) {
            if (onCompleteOnly) {
                return;
            }

            if (onWhen != null && !onWhen.matches(exchange)) {
                // predicate did not match so do not route the onComplete
                return;
            }

            // must use a copy as we dont want it to cause side effects of the original exchange
            final Exchange copy = prepareExchange(exchange);
            // must remove exception otherwise onFailure routing will fail as well
            // the caused exception is stored as a property (Exchange.EXCEPTION_CAUGHT) on the exchange
            copy.setException(null);


            vramelContext.getVertx().runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    LOG.debug("Processing onFailure: {}", copy);
                    doProcess(processor, copy);
                }
            });
        }

        @Override
        public String toString() {
            if (!onCompleteOnly && !onFailureOnly) {
                return "onCompleteOrFailure";
            } else if (onCompleteOnly) {
                return "onCompleteOnly";
            } else {
                return "onFailureOnly";
            }
        }
    }

    @Override
    public String toString() {
        return "OnCompletionProcessor[" + processor + "]";
    }

    public String getTraceLabel() {
        return "onCompletion";
    }
}
