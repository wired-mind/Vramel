package com.nxttxn.vramel.builder;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultVramelContext;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.FlowsDefinition;
import com.nxttxn.vramel.model.ModelVramelContext;
import com.nxttxn.vramel.model.OnExceptionDefinition;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
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
    private AtomicBoolean initialized = new AtomicBoolean(false);
    private FlowsDefinition flowCollection = new FlowsDefinition();



    public VramelContext getVramelContext() {
        return getContext();
    }


    public FlowBuilder() {
        this(null);
    }

    public FlowBuilder(VramelContext context) {
        super(context);
    }


    public abstract void configure() throws Exception;

    @Override
    public String toString() {
        return getFlowCollection().toString();
    }


    protected FlowDefinition from(String uri) {
        getFlowCollection().setVramelContext(getContext());
        final FlowDefinition answer = getFlowCollection().from(uri);
        configureFlow(answer);
        return answer;
    }

    @Override
    public FlowDefinition fromF(String uri, Object... args) {
        getFlowCollection().setVramelContext(getContext());
        final FlowDefinition answer = getFlowCollection().from(String.format(uri, args));
        configureFlow(answer);
        return answer;
    }

    protected FlowDefinition from(Endpoint endpoint) {
        getFlowCollection().setVramelContext(getContext());
        final FlowDefinition answer = getFlowCollection().from(endpoint);
        configureFlow(answer);
        return answer;
    }

    protected FlowDefinition from(String uri, JsonObject config) {
        final ModelVramelContext context = getContext();
        final Endpoint endpoint = context.getEndpoint(uri, config);
        return from(endpoint);
    }

    protected FlowDefinition fromF(String uri, JsonObject config, Object... args) {
        final ModelVramelContext context = getContext();
        final Endpoint endpoint = context.getEndpoint(String.format(uri, args), config);
        return from(endpoint);
    }


    protected void configureFlow(FlowDefinition flow) {
        flow.setGroup(getClass().getName());
    }
    public FlowsDefinition getFlowCollection() {
        return flowCollection;
    }

    @Override
    public void addFlowsToVramelContext(ModelVramelContext vramelContext) throws Exception {
        checkNotNull(vramelContext, "Vramel context is required");

        configureFlows(vramelContext);

        populateRoutes();


    }

    protected void populateRoutes() throws Exception {
        ModelVramelContext vramelContext = getContext();
        if (vramelContext == null) {
            throw new IllegalArgumentException("CamelContext has not been injected!");
        }
        getFlowCollection().setVramelContext(vramelContext);
        vramelContext.addFlowDefinitions(getFlowCollection().getFlows());
    }

    public ModelVramelContext getContext() {
        ModelVramelContext context = super.getContext();
        if (context == null) {
            context = createContainer();
            setContext(context);
        }

        return context;
    }

    /**
     * Factory method
     *
     * @return the CamelContext
     */
    protected ModelVramelContext createContainer() {
        throw new UnsupportedOperationException("Expecting that the context already exists!!!");
    }

        /**
         * Configures the routes
         *
         * @param context the Camel context
         * @return the routes configured
         * @throws Exception can be thrown during configuration
         */
    public FlowsDefinition configureFlows(ModelVramelContext context) throws Exception {
        setContext(context);
        checkInitialized();
        flowCollection.setVramelContext(context);
        return flowCollection;
    }

    @SuppressWarnings("deprecation")
    protected void checkInitialized() throws Exception {
        if (initialized.compareAndSet(false, true)) {
            // Set the CamelContext ErrorHandler here
            VramelContext context = getContext();
            if (context.getErrorHandlerBuilder() != null) {
                setErrorHandlerBuilder(context.getErrorHandlerBuilder());
            }
            configure();

        }
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
        getFlowCollection().setVramelContext(getContext());
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


    protected Config getResolvedConfig() {
        return getVramelContext().getResolvedConfig();
    }

    protected JsonObject getConfig() {
        return new JsonObject(getResolvedConfig().root().render(ConfigRenderOptions.concise()));
    }
    protected JsonObject getConfigObject(String key) {
        return new JsonObject(getResolvedConfig().getObject(key).render(ConfigRenderOptions.concise()));
    }
}
