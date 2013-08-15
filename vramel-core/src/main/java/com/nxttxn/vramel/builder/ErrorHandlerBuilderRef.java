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
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.model.ModelVramelContext;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ObjectHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Represents a proxy to an error handler builder which is resolved by named reference
 *
 * @version
 */
public class ErrorHandlerBuilderRef extends ErrorHandlerBuilderSupport {
    public static final String DEFAULT_ERROR_HANDLER_BUILDER = "CamelDefaultErrorHandlerBuilder";
    private final String ref;
    private final Map<FlowContext, ErrorHandlerBuilder> handlers = new HashMap<FlowContext, ErrorHandlerBuilder>();
    private boolean supportTransacted;

    public ErrorHandlerBuilderRef(String ref) {
        this.ref = ref;
    }

    @Override
    public void addErrorHandlers(FlowContext flowContext, OnExceptionDefinition exception) {
        ErrorHandlerBuilder handler = handlers.get(flowContext);
        if (handler != null) {
            handler.addErrorHandlers(flowContext, exception);
        }
        super.addErrorHandlers(flowContext, exception);
    }

    public Processor createErrorHandler(FlowContext flowContext, Processor processor) throws Exception {
        ErrorHandlerBuilder handler = handlers.get(flowContext);
        if (handler == null) {
            handler = createErrorHandler(flowContext);
            handlers.put(flowContext, handler);
        }
        return handler.createErrorHandler(flowContext, processor);
    }

    public boolean supportTransacted() {
        return supportTransacted;
    }

    @Override
    public ErrorHandlerBuilder cloneBuilder() {
        ErrorHandlerBuilderRef answer = new ErrorHandlerBuilderRef(ref);
        cloneBuilder(answer);
        return answer;
    }

    protected void cloneBuilder(ErrorHandlerBuilderRef other) {
        super.cloneBuilder(other);

        // no need to copy the handlers

        other.supportTransacted = supportTransacted;
    }

    /**
     * Lookup the error handler by the given ref
     *
     * @param flowContext the route context
     * @param ref          reference id for the error handler
     * @return the error handler
     */
    public static ErrorHandlerFactory lookupErrorHandlerBuilder(FlowContext flowContext, String ref) {
        ErrorHandlerFactory answer;

        // if the ref is the default then we do not have any explicit error handler configured
        // if that is the case then use error handlers configured on the route, as for instance
        // the transacted error handler could have been configured on the route so we should use that one
        if (!isErrorHandlerBuilderConfigured(ref)) {
            // see if there has been configured a route builder on the route
            answer = flowContext.getFlow().getErrorHandlerBuilder();
            //no ref support
//            if (answer == null && flowContext.getFlow().getErrorHandlerRef() != null) {
//                answer = flowContext.lookup(flowContext.getFlow().getErrorHandlerRef(), ErrorHandlerBuilder.class);
//            }
            if (answer == null) {
                // fallback to the default error handler if none configured on the route
                answer = new DefaultErrorHandlerBuilder();
            }
            // check if its also a ref with no error handler configuration like me
            if (answer instanceof ErrorHandlerBuilderRef) {
                ErrorHandlerBuilderRef other = (ErrorHandlerBuilderRef) answer;
                String otherRef = other.getRef();
                if (!isErrorHandlerBuilderConfigured(otherRef)) {
                    // the other has also no explicit error handler configured then fallback to the handler
                    // configured on the parent camel context
                    answer = lookupErrorHandlerBuilder((ModelVramelContext)flowContext.getVramelContext());
                }
                if (answer == null) {
                    // the other has also no explicit error handler configured then fallback to the default error handler
                    // otherwise we could recursive loop forever (triggered by createErrorHandler method)
                    answer = new DefaultErrorHandlerBuilder();
                }
                // inherit the error handlers from the other as they are to be shared
                // this is needed by camel-spring when none error handler has been explicit configured
                ((ErrorHandlerBuilder)answer).setErrorHandlers(flowContext, other.getErrorHandlers(flowContext));
            }
        } else {
            // use specific configured error handler
            answer = flowContext.mandatoryLookup(ref, ErrorHandlerBuilder.class);
        }

        return answer;
    }

    protected static ErrorHandlerFactory lookupErrorHandlerBuilder(ModelVramelContext vramelContext) {
        @SuppressWarnings("deprecation")
        ErrorHandlerFactory answer = vramelContext.getErrorHandlerBuilder();
        if (answer instanceof ErrorHandlerBuilderRef) {
            ErrorHandlerBuilderRef other = (ErrorHandlerBuilderRef) answer;
            String otherRef = other.getRef();
            if (isErrorHandlerBuilderConfigured(otherRef)) {
                answer = vramelContext.getRegistry().lookupByNameAndType(otherRef, ErrorHandlerBuilder.class);
                if (answer == null) {
                    throw new IllegalArgumentException("ErrorHandlerBuilder with id " + otherRef + " not found in registry.");
                }
            }
        }

        return answer;
    }

    /**
     * Returns whether a specific error handler builder has been configured or not.
     * <p/>
     * Can be used to test if none has been configured and then install a custom error handler builder
     * replacing the default error handler (that would have been used as fallback otherwise).
     * <br/>
     * This is for instance used by the transacted policy to setup a TransactedErrorHandlerBuilder
     * in camel-spring.
     */
    public static boolean isErrorHandlerBuilderConfigured(String ref) {
        return !DEFAULT_ERROR_HANDLER_BUILDER.equals(ref);
    }

    public String getRef() {
        return ref;
    }

    private ErrorHandlerBuilder createErrorHandler(FlowContext flowContext) {
        ErrorHandlerBuilder handler = (ErrorHandlerBuilder)lookupErrorHandlerBuilder(flowContext, getRef());
        ObjectHelper.notNull(handler, "error handler '" + ref + "'");

        // configure if the handler support transacted
        supportTransacted = handler.supportTransacted();

        List<OnExceptionDefinition> list = getErrorHandlers(flowContext);
        if (list != null) {
            for (OnExceptionDefinition exceptionType : list) {
                handler.addErrorHandlers(flowContext, exceptionType);
            }
        }
        return handler;
    }

    @Override
    public String toString() {
        return "ErrorHandlerBuilderRef[" + ref + "]";
    }
}
