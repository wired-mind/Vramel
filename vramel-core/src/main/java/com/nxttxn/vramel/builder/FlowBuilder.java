package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.FlowsDefinition;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/11/13
 * Time: 5:18 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FlowBuilder extends BuilderSupport implements FlowsBuilder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private FlowsDefinition flowCollection = new FlowsDefinition();
    private AtomicBoolean initialized = new AtomicBoolean(false);

    private VramelContext vramelContext;

    public abstract void configure() throws Exception;

    @Override
    public String toString() {
        return getFlowCollection().toString();
    }


    protected FlowDefinition from(Endpoint endpoint) {
        FlowDefinition flow = getFlowCollection().from(endpoint);
        return flow;
    }

    protected FlowDefinition from(String uri) {
        final Endpoint endpoint = vramelContext.getEndpoint(uri);
        return from(endpoint);
    }

    protected FlowDefinition from(String uri, JsonObject config) {
        final Endpoint endpoint = vramelContext.getEndpoint(uri, config);
        return from(endpoint);
    }

    @Override
    public FlowDefinition fromF(String uri, Object... args) {
        final Endpoint endpoint = vramelContext.getEndpoint(String.format(uri, args));
        return from(endpoint);
    }

    protected FlowDefinition fromF(String uri, JsonObject config, Object... args) {
        final Endpoint endpoint = vramelContext.getEndpoint(String.format(uri, args), config);
        return from(endpoint);
    }


    public FlowsDefinition getFlowCollection() {
        return flowCollection;
    }

    @Override
    public void addFlowsToVramelContext(VramelContext vramelContext) throws Exception {
        checkNotNull(vramelContext, "Vramel context is required");

        configureFlows(vramelContext);


        vramelContext.addFlowDefinitions(getFlowCollection().getFlows());

    }

    /**
     * Configures the routes
     *
     * @param context the Camel context
     * @return the routes configured
     * @throws Exception can be thrown during configuration
     */
    public FlowsDefinition configureFlows(VramelContext context) throws Exception {
        this.vramelContext = context;
        checkInitialized();
        flowCollection.setVramelContext(context);
        return flowCollection;
    }

    @SuppressWarnings("deprecation")
    protected void checkInitialized() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            // Set the CamelContext ErrorHandler here
            VramelContext context = getVramelContext();
            if (context.getErrorHandlerBuilder() != null) {
                setErrorHandlerBuilder(context.getErrorHandlerBuilder());
            }
            configure();

        }
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    protected JsonObject getConfig() {
        return getVramelContext().getConfig();
    }


    @Override
    public void setErrorHandlerBuilder(ErrorHandlerBuilder errorHandlerBuilder) {
        super.setErrorHandlerBuilder(errorHandlerBuilder);
        getFlowCollection().setErrorHandlerBuilder(getErrorHandlerBuilder());
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exception exception to catch
     * @return the builder
     */
    public OnExceptionDefinition onException(Class<? extends Throwable> exception) {
        // is only allowed at the top currently
        if (!getFlowCollection().getFlows().isEmpty()) {
            throw new IllegalArgumentException("onException must be defined before any flows in the FlowBuilder");
        }
        getFlowCollection().setVramelContext(getVramelContext());
        return getFlowCollection().onException(exception);
    }

    /**
     * <a href="http://camel.apache.org/exception-clause.html">Exception clause</a>
     * for catching certain exceptions and handling them.
     *
     * @param exceptions list of exceptions to catch
     * @return the builder
     */
    public OnExceptionDefinition onException(Class<? extends Throwable>... exceptions) {
        OnExceptionDefinition last = null;
        for (Class<? extends Throwable> ex : exceptions) {
            last = last == null ? onException(ex) : last.onException(ex);
        }
        return last != null ? last : onException(Exception.class);
    }

}
