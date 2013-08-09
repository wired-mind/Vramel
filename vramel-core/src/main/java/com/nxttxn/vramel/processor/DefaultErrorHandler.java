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
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.processor.exceptionpolicy.ExceptionPolicyStrategy;
import com.nxttxn.vramel.spi.UnitOfWork;
import com.nxttxn.vramel.util.ExchangeHelper;
import com.nxttxn.vramel.util.VramelLogger;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Default error handler
 */
public class DefaultErrorHandler extends ErrorHandlerSupport {

    private final VramelContext vramelContext;
    private final Processor output;
    private final VramelLogger logger;
    private final ExceptionPolicyStrategy exceptionPolicyStrategy;

    /**
     * Creates the default error handler.
     *
     * @param vramelContext           the camel context
     * @param output                  outer processor that should use this default error handler
     * @param logger                  logger to use for logging failures and redelivery attempts
     * @param exceptionPolicyStrategy strategy for onException handling
     */
    public DefaultErrorHandler(VramelContext vramelContext, Processor output, VramelLogger logger,
                               ExceptionPolicyStrategy exceptionPolicyStrategy) {
        this.vramelContext = vramelContext;
        this.output = output;
        this.logger = logger;
        this.exceptionPolicyStrategy = exceptionPolicyStrategy;


        setExceptionPolicy(exceptionPolicyStrategy);
    }


    @Override
    public String toString() {
        if (output == null) {
            // if no output then dont do any description
            return "";
        }
        return "DefaultErrorHandler[" + output + "]";
    }

    @Override
    public Processor getOutput() {
        return output;
    }

    @Override
    public void process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        checkNotNull(output);
        try {
            output.process(exchange, new OptionalAsyncResultHandler() {
                @Override
                public void handle(AsyncExchangeResult optionalAsyncResult) {

                    final Exchange response = optionalAsyncResult.result.get();
                    if (optionalAsyncResult.failed()) {
                        final Exception exception = response.getException();
                        handleProcessorException(exception, exchange, optionalAsyncResultHandler);
                        return;
                    }

                    optionalAsyncResultHandler.done(response);
                }
            });
        } catch (Exception e) {
            handleProcessorException(e, exchange, optionalAsyncResultHandler);
        }
    }

    private void handleProcessorException(Exception exception, Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        log.error("[DefaultErrorHandler] Exception invoking processor. Most likely an uncaught exception inside the processor.", exception);
        exchange.setException(exception);
        // store the original caused exception in a property, so we can restore it later
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, exception);

        final OnExceptionDefinition exceptionPolicy = getExceptionPolicy(exchange, exchange.getException());
        Processor processor = getFailureProcessor(exchange, exchange.getUnitOfWork(), exceptionPolicy);
        try {
            deliveryToFailureProcessor(processor, exceptionPolicy, exchange, optionalAsyncResultHandler);
        } catch (Exception e) {
            exchange.setException(e);
            optionalAsyncResultHandler.done(exchange);
        }
    }

    private Processor getFailureProcessor(Exchange exchange, UnitOfWork uow, OnExceptionDefinition exceptionPolicy) {
        if (exceptionPolicy == null) {
            return null;
        }
        // find the error handler to use (if any)
        // route specific failure handler?
        Processor processor = null;
        if (uow != null && uow.getFlowContext() != null) {
            String routeId = uow.getFlowContext().getFlow().getId();
            processor = exceptionPolicy.getErrorHandler(routeId);
        } else if (!exceptionPolicy.getErrorHandlers().isEmpty()) {
            // note this should really not happen, but we have this code as a fail safe
            // to be backwards compatible with the old behavior
            log.warn("Cannot determine current route from Exchange with id: {}, will fallback and use first error handler.", exchange.getExchangeId());
            processor = exceptionPolicy.getErrorHandlers().iterator().next();


        }
        return processor;
    }

    private void deliveryToFailureProcessor(final Processor processor, OnExceptionDefinition exceptionPolicy, final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        Exception caught = exchange.getException();

        // we did not success with the redelivery so now we let the failure processor handle it
        // clear exception as we let the failure processor handle it
        exchange.setException(null);

        final boolean shouldHandle = shouldHandled(exchange, exceptionPolicy);
        final boolean shouldContinue = shouldContinue(exchange, exceptionPolicy);
        // regard both handled or continued as being handled
        boolean handled = false;

        if (shouldHandle || shouldContinue) {

            handled = true;
        }

        // is the a failure processor to process the Exchange
        if (processor != null) {

            // prepare original IN body if it should be moved instead of current body
            if (exceptionPolicy.isUseOriginalMessage()) {
                log.trace("Using the original IN message instead of current");
                Message original = exchange.getUnitOfWork().getOriginalInMessage();
                exchange.setIn(original);
                if (exchange.hasOut()) {
                    log.trace("Removing the out message to avoid some uncertain behavior");
                    exchange.setOut(null);
                }
            }

            log.trace("Failure processor {} is processing Exchange: {}", processor, exchange);

            // store the last to endpoint as the failure endpoint
            exchange.setProperty(Exchange.FAILURE_ENDPOINT, exchange.getProperty(Exchange.TO_ENDPOINT));
            // and store the route id so we know in which route we failed
            if (exchange.getUnitOfWork().getFlowContext() != null) {
                exchange.setProperty(Exchange.FAILURE_ROUTE_ID, exchange.getUnitOfWork().getFlowContext().getFlow().getId());
            }

            processor.process(exchange, new OptionalAsyncResultHandler() {
                @Override
                public void handle(AsyncExchangeResult optionalAsyncResult) {
                    log.trace("Failure processor done: {} processing Exchange: {}", processor, exchange);
                    final Exchange result = optionalAsyncResult.result.get();

                    try {
                        prepareExchangeAfterFailure(result, shouldHandle, shouldContinue);
                    } finally {
                        optionalAsyncResultHandler.done(result);
                    }
                }
            });

        } else {
            try {
                // no processor but we need to prepare after failure as well
                prepareExchangeAfterFailure(exchange, shouldHandle, shouldContinue);
            } finally {
                optionalAsyncResultHandler.done(exchange);
            }
        }

    }

    private void prepareExchangeAfterFailure(Exchange exchange, boolean shouldHandle, boolean shouldContinue) {
        // we could not process the exchange so we let the failure processor handled it
        ExchangeHelper.setFailureHandled(exchange);

        // honor if already set a handling
        boolean alreadySet = exchange.getProperty(Exchange.ERRORHANDLER_HANDLED) != null;
        if (alreadySet) {
            boolean handled = exchange.getProperty(Exchange.ERRORHANDLER_HANDLED, Boolean.class);
            log.trace("This exchange has already been marked for handling: {}", handled);
            if (handled) {
                exchange.setException(null);
            } else {
                // exception not handled, put exception back in the exchange
                exchange.setException(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class));
                // and put failure endpoint back as well
                exchange.setProperty(Exchange.FAILURE_ENDPOINT, exchange.getProperty(Exchange.TO_ENDPOINT));
            }
            return;
        }

        if (shouldHandle) {
            log.trace("This exchange is handled so its marked as not failed: {}", exchange);
            exchange.setProperty(Exchange.ERRORHANDLER_HANDLED, Boolean.TRUE);
        } else if (shouldContinue) {
            log.trace("This exchange is continued: {}", exchange);
            // okay we want to continue then prepare the exchange for that as well
            prepareExchangeForContinue(exchange);
        } else {
            log.trace("This exchange is not handled or continued so its marked as failed: {}", exchange);
            // exception not handled, put exception back in the exchange
            exchange.setProperty(Exchange.ERRORHANDLER_HANDLED, Boolean.FALSE);
            exchange.setException(exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class));
            // and put failure endpoint back as well
            exchange.setProperty(Exchange.FAILURE_ENDPOINT, exchange.getProperty(Exchange.TO_ENDPOINT));
            // and store the route id so we know in which route we failed
            if (exchange.getUnitOfWork().getFlowContext() != null) {
                exchange.setProperty(Exchange.FAILURE_ROUTE_ID, exchange.getUnitOfWork().getFlowContext().getFlow().getId());
            }
        }
    }

    private void prepareExchangeForContinue(Exchange exchange) {
        Exception caught = exchange.getException();

        // we continue so clear any exceptions
        exchange.setException(null);

        exchange.removeProperty(Exchange.FAILURE_HANDLED);
        // keep the Exchange.EXCEPTION_CAUGHT as property so end user knows the caused exception

    }

    private boolean shouldContinue(Exchange exchange, OnExceptionDefinition exceptionPolicy) {
        if (exceptionPolicy != null && exceptionPolicy.getContinuedPolicy() != null) {
            return exceptionPolicy.getContinuedPolicy().matches(exchange);
        }
        // do not continue by default
        return false;
    }

    private boolean shouldHandled(Exchange exchange, OnExceptionDefinition exceptionPolicy) {
        if (exceptionPolicy != null && exceptionPolicy.getHandledPolicy() != null) {
            return exceptionPolicy.getHandledPolicy().matches(exchange);
        }
        // do not handle by default
        return false;
    }


    /**
     * Attempts to find the best suited {@link OnExceptionDefinition} to be used for handling the given thrown exception.
     *
     * @param exchange  the exchange
     * @param exception the exception that was thrown
     * @return the best exception type to handle this exception, <tt>null</tt> if none found.
     */
    protected OnExceptionDefinition getExceptionPolicy(Exchange exchange, Throwable exception) {
        if (exceptionPolicy == null) {
            throw new IllegalStateException("The exception policy has not been set");
        }

        return exceptionPolicy.getExceptionPolicy(exceptionPolicies, exchange, exception);
    }
}
