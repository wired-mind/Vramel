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
package com.nxttxn.vramel.processor.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.model.ModelChannel;
import com.nxttxn.vramel.model.ProcessorDefinition;
import com.nxttxn.vramel.processor.FlowContextProcessor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import com.nxttxn.vramel.util.ObjectHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;

public class DefaultChannel extends ServiceSupport implements ModelChannel {

    private static final transient Logger LOG = LoggerFactory.getLogger(DefaultChannel.class);

    private Processor errorHandler;
    // the next processor (non wrapped)
    private Processor nextProcessor;
    // the real output to invoke that has been wrapped
    private Processor output;
    private ProcessorDefinition<?> definition;
    private ProcessorDefinition<?> childDefinition;
    private VramelContext vramelContext;
    private FlowContext flowContext;
    private FlowContextProcessor flowContextProcessor;

    public List<Processor> next() {
        List<Processor> answer = new ArrayList<Processor>(1);
        answer.add(nextProcessor);
        return answer;
    }

    public boolean hasNext() {
        return nextProcessor != null;
    }

    public void setNextProcessor(Processor next) {
        this.nextProcessor = next;
    }

    public Processor getOutput() {
        // the errorHandler is already decorated with interceptors
        // so it contain the entire chain of processors, so we can safely use it directly as output
        // if no error handler provided we use the output
        // TODO: Camel 3.0 we should determine the output dynamically at runtime instead of having the
        // the error handlers, interceptors, etc. woven in at design time
        return errorHandler != null ? errorHandler : output;
    }

    public void setOutput(Processor output) {
        this.output = output;
    }

    public Processor getNextProcessor() {
        return nextProcessor;
    }


    public void setErrorHandler(Processor errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Processor getErrorHandler() {
        return errorHandler;
    }



    public ProcessorDefinition<?> getProcessorDefinition() {
        return definition;
    }

    public void setChildDefinition(ProcessorDefinition<?> childDefinition) {
        this.childDefinition = childDefinition;
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    @Override
    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        // create route context processor to wrap output
        flowContextProcessor = new FlowContextProcessor(flowContext, getOutput());
        ServiceHelper.startServices(errorHandler, output, flowContextProcessor);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopServices(output, errorHandler, flowContextProcessor);
    }


    public void initChannel(ProcessorDefinition<?> outputDefinition, FlowContext flowContext) throws Exception {
        this.flowContext = flowContext;
        this.definition = outputDefinition;
        this.vramelContext = flowContext.getVramelContext();

        Processor target = nextProcessor;
        Processor next;

        // init CamelContextAware as early as possible on target
        if (target instanceof VramelContextAware) {
            ((VramelContextAware) target).setVramelContext(vramelContext);
        }

        // the definition to wrap should be the fine grained,
        // so if a child is set then use it, if not then its the original output used
        ProcessorDefinition<?> targetOutputDef = childDefinition != null ? childDefinition : outputDefinition;
        LOG.debug("Initialize channel for target: '{}'", targetOutputDef);

        // fix parent/child relationship. This will be the case of the routes has been
        // defined using XML DSL or end user may have manually assembled a route from the model.
        // Background note: parent/child relationship is assembled on-the-fly when using Java DSL (fluent builders)
        // where as when using XML DSL (JAXB) then it fixed after, but if people are using custom interceptors
        // then we need to fix the parent/child relationship beforehand, and thus we can do it here
        // ideally we need the design time route -> runtime route to be a 2-phase pass (scheduled work for Camel 3.0)
        if (childDefinition != null && outputDefinition != childDefinition) {
            childDefinition.setParent(outputDefinition);
        }


        // sets the delegate to our wrapped output
        output = target;
    }

    @Override
    public void postInitChannel(ProcessorDefinition<?> outputDefinition, FlowContext flowContext) throws Exception {


    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }


    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

        Processor processor = getOutput();
        if (processor == null || !continueProcessing(exchange)) {
            // we should not continue routing so we are done
            optionalAsyncResultHandler.done(exchange);
            return true;
        }

        // process the exchange using the route context processor
        ObjectHelper.notNull(flowContextProcessor, "FlowContextProcessor", this);
        return flowContextProcessor.process(exchange, optionalAsyncResultHandler);
    }

    private boolean continueProcessing(Exchange exchange) {
        Object stop = exchange.getProperty(Exchange.ROUTE_STOP);
        if (stop != null) {
            boolean doStop = exchange.getContext().getTypeConverter().convertTo(Boolean.class, stop);
            if (doStop) {
                LOG.debug("Exchange is marked to stop routing: {}", exchange);
                return false;
            }
        }

//        // determine if we can still run, or the camel context is forcing a shutdown
//        boolean forceShutdown = vramelContext.getShutdownStrategy().forceShutdown(this);
//        if (forceShutdown) {
//            LOG.debug("Run not allowed as ShutdownStrategy is forcing shutting down, will reject executing exchange: {}", exchange);
//            if (exchange.getException() == null) {
//                exchange.setException(new RejectedExecutionException());
//            }
//            return false;
//        }

        // yes we can continue
        return true;
    }


    @Override
    public String toString() {
        // just output the next processor as all the interceptors and error handler is just too verbose
        return "Channel[" + nextProcessor + "]";
    }

}
