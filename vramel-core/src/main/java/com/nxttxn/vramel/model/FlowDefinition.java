package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ErrorHandlerBuilderRef;import com.nxttxn.vramel.impl.DefaultFlowContext;import com.nxttxn.vramel.spi.FlowContext;
import com.nxttxn.vramel.util.FlowDefinitionHelper;
import com.nxttxn.vramel.util.VramelContextHelper;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class FlowDefinition extends ProcessorDefinition<FlowDefinition> {

    private final AtomicBoolean prepared = new AtomicBoolean(false);
    private List<ProcessorDefinition<?>> outputs = Lists.newArrayList();
    private List<FromDefinition> inputs = new ArrayList<FromDefinition>();
    private String group;
    private String autoStartup;
    private Integer startupOrder;
    private ErrorHandlerFactory errorHandlerBuilder;
    // keep state whether the error handler is context scoped or not
    // (will by default be context scoped of no explicit error handler configured)
    private boolean contextScopedErrorHandler = true;

    public FlowDefinition(String uri) {
        from(uri);
    }
    public FlowDefinition(Endpoint endpoint) {
        from(endpoint);
    }

    public FlowDefinition() {

    }

    @Override
    public String toString() {
        if (getId() != null) {
            return "Flow(" + getId() + ")[" + inputs + " -> " + outputs + "]";
        } else {
            return "Flow[" + inputs + " -> " + outputs + "]";
        }
    }

    public String getAutoStartup() {
        return autoStartup;
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
     * Disables this route from being auto started when Camel starts.
     *
     * @return the builder
     */
    public FlowDefinition noAutoStartup() {
        setAutoStartup("false");
        return this;
    }

    /**
     * Sets the auto startup property on this route.
     *
     * @param autoStartup - String indicator ("true" or "false")
     * @return the builder
     */
    public FlowDefinition autoStartup(String autoStartup) {
        setAutoStartup(autoStartup);
        return this;
    }

    /**
     * Sets the auto startup property on this route.
     *
     * @param autoStartup - boolean indicator
     * @return the builder
     */
    public FlowDefinition autoStartup(boolean autoStartup) {
        setAutoStartup(Boolean.toString(autoStartup));
        return this;
    }

    /**
     * Set the group name for this route
     *
     * @param name the group name
     * @return the builder
     */
    public FlowDefinition group(String name) {
        setGroup(name);
        return this;
    }

    /**
     * Set the route id for this route
     *
     * @param id the route id
     * @return the builder
     */
    public FlowDefinition routeId(String id) {
        setId(id);
        return this;
    }

    /**
     * Configures the startup order for this route
     * <p/>
     * Camel will reorder routes and star them ordered by 0..N where 0 is the lowest number and N the highest number.
     * Camel will stop routes in reverse order when its stopping.
     *
     * @param order the order represented as a number
     * @return the builder
     */
    public FlowDefinition startupOrder(int order) {
        setStartupOrder(order);
        return this;
    }

    /**
     * The group that this route belongs to; could be the name of the RouteBuilder class
     * or be explicitly configured in the XML.
     * <p/>
     * May be null.
     */
    public String getGroup() {
        return group;
    }

    @XmlAttribute
    public void setGroup(String group) {
        this.group = group;
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

    private FlowContext addFlows(VramelContext vramelContext, Collection<Flow> flows, FromDefinition fromType) throws Exception {
        FlowContext flowContext = new DefaultFlowContext(vramelContext, this, fromType, flows);


        // configure auto startup
        Boolean isAutoStartup = VramelContextHelper.parseBoolean(vramelContext, getAutoStartup());
        if (isAutoStartup != null) {
            log.debug("Using AutoStartup {} on route: {}", isAutoStartup, getId());
            flowContext.setAutoStartup(isAutoStartup);
        }


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

    @SuppressWarnings("deprecation")
    public boolean isContextScopedErrorHandler(VramelContext context) {
        if (!contextScopedErrorHandler) {
            return false;
        }
//        // if error handler ref is configured it may refer to a context scoped, so we need to check this first
//        // the XML DSL will configure error handlers using refs, so we need this additional test
//        if (errorHandlerRef != null) {
//            ErrorHandlerFactory routeScoped = getErrorHandlerBuilder();
//            ErrorHandlerFactory contextScoped = context.getErrorHandlerBuilder();
//            return routeScoped != null && contextScoped != null && routeScoped == contextScoped;
//        }

        return contextScopedErrorHandler;
    }

    /**
     * Prepares the route definition to be ready to be added to {@link VramelContext}
     *
     * @param context the camel context
     */
    public void prepare(ModelVramelContext context) {
        if (prepared.compareAndSet(false, true)) {
            FlowDefinitionHelper.prepareRoute(context, this);
        }
    }

    public boolean isAutoStartup(VramelContext camelContext) throws Exception {
        if (getAutoStartup() == null) {
            // should auto startup by default
            return true;
        }
        Boolean isAutoStartup = VramelContextHelper.parseBoolean(camelContext, getAutoStartup());
        return isAutoStartup != null && isAutoStartup;
    }


    @XmlAttribute
    public void setAutoStartup(String autoStartup) {
        this.autoStartup = autoStartup;
    }

    public Integer getStartupOrder() {
        return startupOrder;
    }

    @XmlAttribute
    public void setStartupOrder(Integer startupOrder) {
        this.startupOrder = startupOrder;
    }

}
