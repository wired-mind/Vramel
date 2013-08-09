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

import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.processor.ErrorHandler;
import com.nxttxn.vramel.processor.ErrorHandlerSupport;
import com.nxttxn.vramel.processor.exceptionpolicy.ExceptionPolicyStrategy;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ObjectHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Base class for builders of error handling.
 *
 * @version
 */
public abstract class ErrorHandlerBuilderSupport implements ErrorHandlerBuilder {
    private Map<FlowContext, List<OnExceptionDefinition>> onExceptions = new HashMap<FlowContext, List<OnExceptionDefinition>>();
    private ExceptionPolicyStrategy exceptionPolicyStrategy;

    public void addErrorHandlers(FlowContext flowContext, OnExceptionDefinition exception) {
        // only add if we not already have it
        List<OnExceptionDefinition> list = onExceptions.get(flowContext);
        if (list == null) {
            list = new ArrayList<OnExceptionDefinition>();
            onExceptions.put(flowContext, list);
        }
        if (!list.contains(exception)) {
            list.add(exception);
        }
    }

    protected void cloneBuilder(ErrorHandlerBuilderSupport other) {
        if (!onExceptions.isEmpty()) {
            Map<FlowContext, List<OnExceptionDefinition>> copy = new HashMap<FlowContext, List<OnExceptionDefinition>>(onExceptions);
            other.onExceptions = copy;
        }
        other.exceptionPolicyStrategy = exceptionPolicyStrategy;
    }

    public void configure(FlowContext flowContext, ErrorHandler handler) {
        if (handler instanceof ErrorHandlerSupport) {
            ErrorHandlerSupport handlerSupport = (ErrorHandlerSupport) handler;

            List<OnExceptionDefinition> list = onExceptions.get(flowContext);
            if (list != null) {
                for (OnExceptionDefinition exception : list) {
                    handlerSupport.addExceptionPolicy(flowContext, exception);
                }
            }
        }
    }

    public List<OnExceptionDefinition> getErrorHandlers(FlowContext flowContext) {
        return onExceptions.get(flowContext);
    }

    public void setErrorHandlers(FlowContext flowContext, List<OnExceptionDefinition> exceptions) {
        this.onExceptions.put(flowContext, exceptions);
    }

    /**
     * Sets the exception policy to use
     */
    public ErrorHandlerBuilderSupport exceptionPolicyStrategy(ExceptionPolicyStrategy exceptionPolicyStrategy) {
        setExceptionPolicyStrategy(exceptionPolicyStrategy);
        return this;
    }

    public ExceptionPolicyStrategy  getExceptionPolicyStrategy() {
        return exceptionPolicyStrategy;
    }

    public void setExceptionPolicyStrategy(ExceptionPolicyStrategy exceptionPolicyStrategy) {
        ObjectHelper.notNull(exceptionPolicyStrategy, "ExceptionPolicyStrategy");
        this.exceptionPolicyStrategy = exceptionPolicyStrategy;
    }
}
