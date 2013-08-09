package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ErrorHandlerBuilderRef;import com.nxttxn.vramel.impl.DefaultFlowContext;import com.nxttxn.vramel.spi.FlowContext;

import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlowDefinition extends ProcessorDefinition<FlowDefinition> {

    private List<ProcessorDefinition<?>> outputs = Lists.newArrayList();
    private List<FromDefinition> inputs = new ArrayList<FromDefinition>();
    private ErrorHandlerFactory errorHandlerBuilder;

    public FlowDefinition(String uri) {
        from(uri);
    }
    public FlowDefinition(Endpoint endpoint) {
        from(endpoint);
    }

    public FlowDefinition() {

    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return outputs;
    }

    /**
     * Creates an input to the route
     *
     * @param uri the from uri
     * @return the builder
     */
    public FlowDefinition from(String uri) {
        getInputs().add(new FromDefinition(uri));
        return this;
    }

    /**
     * Creates an input to the route
     *
     * @param endpoint the from endpoint
     * @return the builder
     */
    public FlowDefinition from(Endpoint endpoint) {
        getInputs().add(new FromDefinition(endpoint));
        return this;
    }

    public List<FlowContext> addFlows(VramelContext vramelContext, Collection<Flow> flows) throws FailedToCreateRouteException {
        List<FlowContext> answer = new ArrayList<FlowContext>();

        @SuppressWarnings("deprecation")
        ErrorHandlerFactory handler = vramelContext.getErrorHandlerBuilder();
        if (handler != null) {
            setErrorHandlerBuilderIfNull(handler);
        }

        for (FromDefinition fromType : getInputs()) {
            FlowContext flowContext;
            try {
                flowContext = addFlows(vramelContext, flows, fromType);
            } catch (FailedToCreateRouteException e) {
                throw e;
            } catch (Exception e) {
                // wrap in exception which provide more details about which route was failing
                throw new FailedToCreateRouteException("noid", toString(), e);
            }
            answer.add(flowContext);
        }
        return answer;
    }

    /**
     * Sets the error handler if one is not already set
     */
    public void setErrorHandlerBuilderIfNull(ErrorHandlerFactory errorHandlerBuilder) {
        if (this.errorHandlerBuilder == null) {
            setErrorHandlerBuilder(errorHandlerBuilder);
        }
    }

    public void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder) {
        this.errorHandlerBuilder = errorHandlerBuilder;
    }

    private FlowContext addFlows(VramelContext vramelContext, Collection<Flow> flows, FromDefinition fromType) throws FailedToCreateRouteException {
        FlowContext flowContext = new DefaultFlowContext(vramelContext, this, fromType, flows);



        // validate route has output processors
        if (!ProcessorDefinitionHelper.hasOutputs(outputs)) {
            FlowDefinition flow = flowContext.getFlow();
            String at = fromType.toString();
            Exception cause = new IllegalArgumentException("Route " + flow.getId() + " has no output processors."
                    + " You need to add outputs to the flow such as to(\"log:foo\").");
            throw new FailedToCreateRouteException(flow.getId(), flow.toString(), at, cause);
        }

        List<ProcessorDefinition<?>> list = new ArrayList<ProcessorDefinition<?>>(outputs);
        for (ProcessorDefinition<?> output : list) {
            try {
                output.addFlows(flowContext, flows);
            } catch (Exception e) {
                FlowDefinition flow = flowContext.getFlow();
                throw new FailedToCreateRouteException(flow.getId(), flow.toString(), output.toString(), e);
            }
        }

        flowContext.commit();
        return flowContext;
    }

    public List<FromDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(List<FromDefinition> inputs) {
        this.inputs = inputs;
    }


    @XmlTransient
    public ErrorHandlerFactory getErrorHandlerBuilder() {
        if (errorHandlerBuilder == null) {
            errorHandlerBuilder = createErrorHandlerBuilder();
        }
        return errorHandlerBuilder;
    }

    private ErrorHandlerFactory createErrorHandlerBuilder() {

        // return a reference to the default error handler
        return new ErrorHandlerBuilderRef(ErrorHandlerBuilderRef.DEFAULT_ERROR_HANDLER_BUILDER);
    }

}
