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
package com.nxttxn.vramel.model;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.processor.CatchProcessor;
import com.nxttxn.vramel.processor.FatalFallbackErrorHandler;
import com.nxttxn.vramel.spi.ClassResolver;
import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.ExpressionToPredicateAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;


/**
 * Represents an XML &lt;onException/&gt; element
 *
 * @version
 */
@XmlRootElement(name = "onException")
@XmlAccessorType(XmlAccessType.FIELD)
public class OnExceptionDefinition extends ProcessorDefinition<OnExceptionDefinition> {
    @XmlElement(name = "exception", required = true)
    private List<String> exceptions = new ArrayList<String>();
    @XmlElement(name = "onWhen")
    private WhenDefinition onWhen;
     @XmlElement(name = "handled")
    private ExpressionSubElementDefinition handled;
    @XmlElement(name = "continued")
    private ExpressionSubElementDefinition continued;
    @XmlAttribute(name = "onRedeliveryRef")
    private String onRedeliveryRef;
    @XmlAttribute(name = "useOriginalMessage")
    private Boolean useOriginalMessagePolicy;
    @XmlElementRef
    private List<ProcessorDefinition<?>> outputs = new ArrayList<ProcessorDefinition<?>>();
    @XmlTransient
    private List<Class<? extends Throwable>> exceptionClasses;
    @XmlTransient
    private Predicate handledPolicy;
    @XmlTransient
    private Predicate continuedPolicy;
    @XmlTransient
    private Boolean flowScoped;
    // TODO: in Camel 3.0 the OnExceptionDefinition should not contain state and ErrorHandler processors
    @XmlTransient
    private final Map<String, Processor> errorHandlers = new HashMap<String, Processor>();

    public OnExceptionDefinition() {
    }

    public OnExceptionDefinition(List<Class<? extends Throwable>> exceptionClasses) {
        this.exceptionClasses = exceptionClasses;
    }

    public OnExceptionDefinition(Class<? extends Throwable> exceptionType) {
        exceptionClasses = new ArrayList<Class<? extends Throwable>>();
        exceptionClasses.add(exceptionType);
    }

    public void setFlowScoped(boolean flowScoped) {
        this.flowScoped = flowScoped;
    }

    public boolean isFlowScoped() {
        // is context scoped by default
        return flowScoped != null ? flowScoped : false;
    }


    @Override
    public String toString() {
        return "OnException[" + description() + " -> " + getOutputs() + "]";
    }

    protected String description() {
        return getExceptionClasses() + (onWhen != null ? " " + onWhen : "");
    }

    @Override
    public String getLabel() {
        return "onException[" + description() + "]";
    }




    public void addFlows(FlowContext flowContext, Collection<Flow> flows) throws Exception {
        // assign whether this was a route scoped onException or not
        // we need to know this later when setting the parent, as only route scoped should have parent
        // Note: this logic can possible be removed when the Camel routing engine decides at runtime
        // to apply onException in a more dynamic fashion than current code base
        // and therefore is in a better position to decide among context/route scoped OnException at runtime
        if (flowScoped == null) {
            flowScoped = super.getParent() != null;
        }

        setHandledFromExpressionType(flowContext);
        setContinuedFromExpressionType(flowContext);


        // load exception classes
        if (exceptions != null && !exceptions.isEmpty()) {
            exceptionClasses = createExceptionClasses(flowContext.getVramelContext().getClassResolver());
        }

        // must validate configuration before creating processor
        validateConfiguration();

        // lets attach this on exception to the route error handler

        Processor child = createOutputsProcessor(flowContext);
        if (child != null) {
            // wrap in our special safe fallback error handler if OnException have child output
            Processor errorHandler = new FatalFallbackErrorHandler(child);
            String id = flowContext.getFlow().getId();
            errorHandlers.put(id, errorHandler);
        }
        // lookup the error handler builder
        ErrorHandlerBuilder builder = (ErrorHandlerBuilder)flowContext.getFlow().getErrorHandlerBuilder();
        // and add this as error handlers
        builder.addErrorHandlers(flowContext, this);
    }

    @Override
    public CatchProcessor createProcessor(FlowContext flowContext) throws Exception {
        // load exception classes
        if (exceptions != null && !exceptions.isEmpty()) {
            exceptionClasses = createExceptionClasses(flowContext.getVramelContext().getClassResolver());
        }

        // must validate configuration before creating processor
        validateConfiguration();

        Processor childProcessor = this.createChildProcessor(flowContext);

        Predicate when = null;
        if (onWhen != null) {
            when = onWhen.getExpression().createPredicate(flowContext);
        }

        Predicate handle = null;
        if (handled != null) {
            handle = handled.createPredicate(flowContext);
        }

        return new CatchProcessor(getExceptionClasses(), childProcessor, when, handle);
    }

    protected void validateConfiguration() {
        if (isInheritErrorHandler() != null && isInheritErrorHandler()) {
            throw new IllegalArgumentException(this + " cannot have the inheritErrorHandler option set to true");
        }

        List<Class<? extends Throwable>> exceptions = getExceptionClasses();
        if (exceptions == null || exceptions.isEmpty()) {
            throw new IllegalArgumentException("At least one exception must be configured on " + this);
        }

        // only one of handled or continued is allowed
        if (getHandledPolicy() != null && getContinuedPolicy() != null) {
            throw new IllegalArgumentException("Only one of handled or continued is allowed to be configured on: " + this);
        }

        // validate that at least some option is set as you cannot just have onException(Exception.class);
        if (outputs == null || getOutputs().isEmpty()) {
            // no outputs so there should be some sort of configuration
            if (handledPolicy == null && continuedPolicy == null && useOriginalMessagePolicy == null) {
                throw new IllegalArgumentException(this + " is not configured.");
            }
        }
    }

    // Fluent API
    //-------------------------------------------------------------------------

    @Override
    public OnExceptionDefinition onException(Class<? extends Throwable> exceptionType) {
        getExceptionClasses().add(exceptionType);
        return this;
    }

    /**
     * Sets whether the exchange should be marked as handled or not.
     *
     * @param handled handled or not
     * @return the builder
     */
    public OnExceptionDefinition handled(boolean handled) {
        Expression expression = ExpressionBuilder.constantExpression(Boolean.toString(handled));
        return handled(expression);
    }

    /**
     * Sets whether the exchange should be marked as handled or not.
     *
     * @param handled predicate that determines true or false
     * @return the builder
     */
    public OnExceptionDefinition handled(Predicate handled) {
        setHandledPolicy(handled);
        return this;
    }

    /**
     * Sets whether the exchange should be marked as handled or not.
     *
     * @param handled expression that determines true or false
     * @return the builder
     */
    public OnExceptionDefinition handled(Expression handled) {
        setHandledPolicy(ExpressionToPredicateAdapter.toPredicate(handled));
        return this;
    }

    /**
     * Sets whether the exchange should handle and continue routing from the point of failure.
     * <p/>
     * If this option is enabled then its considered handled as well.
     *
     * @param continued continued or not
     * @return the builder
     */
    public OnExceptionDefinition continued(boolean continued) {
        Expression expression = ExpressionBuilder.constantExpression(Boolean.toString(continued));
        return continued(expression);
    }

    /**
     * Sets whether the exchange should be marked as handled or not.
     * <p/>
     * If this option is enabled then its considered handled as well.
     *
     * @param continued predicate that determines true or false
     * @return the builder
     */
    public OnExceptionDefinition continued(Predicate continued) {
        setContinuedPolicy(continued);
        return this;
    }

    /**
     * Sets whether the exchange should be marked as handled or not.
     * <p/>
     * If this option is enabled then its considered handled as well.
     *
     * @param continued expression that determines true or false
     * @return the builder
     */
    public OnExceptionDefinition continued(Expression continued) {
        setContinuedPolicy(ExpressionToPredicateAdapter.toPredicate(continued));
        return this;
    }

    /**
     * Sets an additional predicate that should be true before the onException is triggered.
     * <p/>
     * To be used for fine grained controlling whether a thrown exception should be intercepted
     * by this exception type or not.
     *
     * @param predicate predicate that determines true or false
     * @return the builder
     */
    public OnExceptionDefinition onWhen(Predicate predicate) {
        setOnWhen(new WhenDefinition(predicate));
        return this;
    }



    /**
     * Will use the original input message when an {@link org.apache.camel.Exchange} is moved to the dead letter queue.
     * <p/>
     * <b>Notice:</b> this only applies when all redeliveries attempt have failed and the {@link org.apache.camel.Exchange} is doomed for failure.
     * <br/>
     * Instead of using the current inprogress {@link org.apache.camel.Exchange} IN body we use the original IN body instead. This allows
     * you to store the original input in the dead letter queue instead of the inprogress snapshot of the IN body.
     * For instance if you route transform the IN body during routing and then failed. With the original exchange
     * store in the dead letter queue it might be easier to manually re submit the {@link org.apache.camel.Exchange} again as the IN body
     * is the same as when Camel received it. So you should be able to send the {@link org.apache.camel.Exchange} to the same input.
     * <p/>
     * By default this feature is off.
     *
     * @return the builder
     */
    public OnExceptionDefinition useOriginalMessage() {
        setUseOriginalMessagePolicy(Boolean.TRUE);
        return this;
    }


    // Properties
    //-------------------------------------------------------------------------
    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ProcessorDefinition<?>> outputs) {
        this.outputs = outputs;
    }

    public boolean isOutputSupported() {
        return true;
    }

    public List<Class<? extends Throwable>> getExceptionClasses() {
        return exceptionClasses;
    }

    public void setExceptionClasses(List<Class<? extends Throwable>> exceptionClasses) {
        this.exceptionClasses = exceptionClasses;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }

    public Processor getErrorHandler(String routeId) {
        return errorHandlers.get(routeId);
    }

    public Collection<Processor> getErrorHandlers() {
        return errorHandlers.values();
    }


    public Predicate getHandledPolicy() {
        return handledPolicy;
    }

    public void setHandled(ExpressionSubElementDefinition handled) {
        this.handled = handled;
    }

    public ExpressionSubElementDefinition getContinued() {
        return continued;
    }

    public void setContinued(ExpressionSubElementDefinition continued) {
        this.continued = continued;
    }

    public ExpressionSubElementDefinition getHandled() {
        return handled;
    }

    public void setHandledPolicy(Predicate handledPolicy) {
        this.handledPolicy = handledPolicy;
    }

    public Predicate getContinuedPolicy() {
        return continuedPolicy;
    }

    public void setContinuedPolicy(Predicate continuedPolicy) {
        this.continuedPolicy = continuedPolicy;
    }

    public WhenDefinition getOnWhen() {
        return onWhen;
    }

    public void setOnWhen(WhenDefinition onWhen) {
        this.onWhen = onWhen;
    }



    public Boolean getUseOriginalMessagePolicy() {
        return useOriginalMessagePolicy;
    }

    public void setUseOriginalMessagePolicy(Boolean useOriginalMessagePolicy) {
        this.useOriginalMessagePolicy = useOriginalMessagePolicy;
    }

    public boolean isUseOriginalMessage() {
        return useOriginalMessagePolicy != null && useOriginalMessagePolicy;
    }


    // Implementation methods
    //-------------------------------------------------------------------------


    protected List<Class<? extends Throwable>> createExceptionClasses(ClassResolver resolver) throws ClassNotFoundException {
        List<String> list = getExceptions();
        List<Class<? extends Throwable>> answer = new ArrayList<Class<? extends Throwable>>(list.size());
        for (String name : list) {
            Class<? extends Throwable> type = resolver.resolveMandatoryClass(name, Throwable.class);
            answer.add(type);
        }
        return answer;
    }

    private void setHandledFromExpressionType(FlowContext flowContext) {
        if (getHandled() != null && handledPolicy == null && flowContext != null) {
            handled(getHandled().createPredicate(flowContext));
        }
    }

    private void setContinuedFromExpressionType(FlowContext flowContext) {
        if (getContinued() != null && continuedPolicy == null && flowContext != null) {
            continued(getContinued().createPredicate(flowContext));
        }
    }



}
