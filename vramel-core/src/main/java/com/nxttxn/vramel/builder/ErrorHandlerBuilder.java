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
package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.ErrorHandlerFactory;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.processor.ErrorHandler;
import com.nxttxn.vramel.processor.exceptionpolicy.ExceptionPolicyStrategy;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.List;



/**
 * A builder of a <a href="http://camel.apache.org/error-handler.html">Error Handler</a>
 *
 * @version
 */
public interface ErrorHandlerBuilder extends ErrorHandlerFactory {

    /**
     * Adds error handler for the given exception type
     *
     * @param flowContext  the route context
     * @param exception     the exception to handle
     */
    void addErrorHandlers(FlowContext flowContext, OnExceptionDefinition exception);

    /**
     * Adds the error handlers for the given list of exception types
     *
     * @param flowContext  the route context
     * @param exceptions    the list of exceptions to handle
     */
    void setErrorHandlers(FlowContext flowContext, List<OnExceptionDefinition> exceptions);

    /**
     * Gets the error handlers
     *
     * @param flowContext  the route context
     */
    List<OnExceptionDefinition> getErrorHandlers(FlowContext flowContext);

    /**
     * Gets the exception policy strategy
     */
    ExceptionPolicyStrategy getExceptionPolicyStrategy();

    /**
     * Sets the exception policy strategy to use for resolving the {@link org.apache.camel.model.OnExceptionDefinition}
     * to use for a given thrown exception
     *
     * @param exceptionPolicyStrategy  the exception policy strategy
     */
    void setExceptionPolicyStrategy(ExceptionPolicyStrategy exceptionPolicyStrategy);

    /**
     * Whether this error handler supports transacted exchanges.
     */
    boolean supportTransacted();

    /**
     * Configures the other error handler based on this error handler.
     *
     * @param flowContext the route context
     * @param handler the other error handler
     */
    void configure(FlowContext flowContext, ErrorHandler handler);

    /**
     * Clones this builder so each {@link FlowBuilder} has its private builder
     * to use, to avoid changes from one {@link FlowBuilder} to influence the
     * others.
     * <p/>
     * This is needed by the current Camel 2.x architecture.
     *
     * @return a clone of this {@link ErrorHandlerBuilder}
     */
    ErrorHandlerBuilder cloneBuilder();
}
