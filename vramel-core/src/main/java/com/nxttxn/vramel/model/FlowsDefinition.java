package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ErrorHandlerFactory;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.FlowDefinitionHelper;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlowsDefinition {
    @XmlElementRef
    private List<FlowDefinition> flows = Lists.newArrayList();
    @XmlTransient
    private List<OnExceptionDefinition> onExceptions = new ArrayList<OnExceptionDefinition>();
    @XmlTransient
    private ErrorHandlerFactory errorHandlerBuilder;

    private VramelContext vramelContext;

    public List<OnExceptionDefinition> getOnExceptions() {
        return onExceptions;
    }

    @Override
    public String toString() {
        return "Flows: " + flows;
    }

    public void setOnExceptions(List<OnExceptionDefinition> onExceptions) {
        this.onExceptions = onExceptions;
    }
    public ErrorHandlerFactory getErrorHandlerBuilder() {
        return errorHandlerBuilder;
    }

    public void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder) {
        this.errorHandlerBuilder = errorHandlerBuilder;
    }

    public FlowDefinition from(Endpoint endpoint) {
        FlowDefinition flow = createFlow();
        flow.from(endpoint);
        return flow(flow);
    }

    public FlowDefinition from(String uri) {
        FlowDefinition flow = createFlow();
        flow.from(uri);
        return flow(flow);
    }

    private FlowDefinition createFlow() {
        final FlowDefinition flowDefinition = new FlowDefinition();
        ErrorHandlerFactory handler = getErrorHandlerBuilder();
        if (handler != null) {
            flowDefinition.setErrorHandlerBuilderIfNull(handler);
        }
        return flowDefinition;
    }

    public List<FlowDefinition> getFlows() {
        return flows;
    }

    /**
     * Creates a new flow using the given flow
     *
     * @param flow the flow
     * @return the builder
     */
    public FlowDefinition flow(FlowDefinition flow) {
        // must prepare the flow before we can add it to the routes list
        FlowDefinitionHelper.prepareRoute(getVramelContext(), flow, getOnExceptions());

        getFlows().add(flow);
        // mark this flow as prepared
//        flow.markPrepared();
        return flow;
    }

    /**
     * Adds an on exception
     *
     * @param exception  the exception
     * @return the builder
     */
    public OnExceptionDefinition onException(Class<? extends Throwable> exception) {
        OnExceptionDefinition answer = new OnExceptionDefinition(exception);
        answer.setFlowScoped(false);
        getOnExceptions().add(answer);
        return answer;
    }

    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }


}
