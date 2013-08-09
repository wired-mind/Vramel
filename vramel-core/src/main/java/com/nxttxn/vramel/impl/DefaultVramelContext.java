package com.nxttxn.vramel.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.components.properties.PropertiesComponent;
import com.nxttxn.vramel.language.bean.BeanLanguage;
import com.nxttxn.vramel.language.simple.SimpleLanguage;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.ModelVramelContext;
import com.nxttxn.vramel.spi.*;
import com.nxttxn.vramel.util.FlowDefinitionHelper;
import com.nxttxn.vramel.util.ObjectHelper;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultVramelContext implements ModelVramelContext {
    private final Logger logger = LoggerFactory.getLogger(DefaultVramelContext.class);
    private final BusModBase busModBase;
    private final String defaultEndpointConfig = "default_endpoint_config";
    private List<FlowDefinition> flowDefinitions = Lists.newArrayList();
    private DefaultServerFactory defaultServerFactory;
    private ClientFactory defaultClientFactory;
    private List<Consumer> consumers = Lists.newArrayList();
    private Map<String, Component> components = Maps.newHashMap();
    private JsonObject config;
    private final HashMap<String, Language> languages = new HashMap<String, Language>() {{
        put("bean", new BeanLanguage());
        put("simple", new SimpleLanguage());
    }};
    private ErrorHandlerFactory errorHandlerBuilder;
    private List<FlowContext> flowContexts = Lists.newArrayList();
    private NodeIdFactory nodeIdFactory = new DefaultNodeIdFactory();
    private UuidGenerator uuidGenerator = createDefaultUuidGenerator();
    private Map<String, String> properties = new HashMap<String, String>();
    private PropertiesComponent propertiesComponent;

    private UuidGenerator createDefaultUuidGenerator() {
        return new JavaUuidGenerator();
    }

    public DefaultVramelContext(BusModBase busModBase) {

        this.busModBase = busModBase;
        this.config = busModBase.getContainer().getConfig();
        this.defaultServerFactory = new DefaultServerFactory(busModBase.getVertx());
        this.defaultClientFactory = new DefaultClientFactory(busModBase.getVertx());
    }


    @Override
    public void addFlowBuilder(FlowsBuilder flowsBuilder) throws Exception {
        flowsBuilder.addFlowsToVramelContext(this);
    }

    @Override
    public void run() throws Exception {

        getServerFactory().startAllServers();
    }

    @Override
    public void addFlowDefinitions(List<FlowDefinition> flowDefinitions) throws Exception {
        this.flowDefinitions.addAll(flowDefinitions);

        startFlowDefinitions(flowDefinitions);
    }

    private void startFlowDefinitions(List<FlowDefinition> list) throws Exception {
        if (list != null) {
            for (FlowDefinition flow : list) {
                startFlow(flow);
            }
        }
    }

    private void startFlow(FlowDefinition flowDefinition) throws Exception {

        // assign ids to the routes and validate that the id's is all unique
        FlowDefinitionHelper.forceAssignIds(this, flowDefinitions);
        String duplicate = FlowDefinitionHelper.validateUniqueIds(flowDefinition, flowDefinitions);
        if (duplicate != null) {
            throw new FailedToStartFlowException(flowDefinition.getId(), "duplicate id detected: " + duplicate + ". Please correct ids to be unique among all your routes.");
        }

        // must ensure route is prepared, before we can start it
        //flowDefinition.prepare(this);


        List<Flow> flows = new ArrayList<Flow>();
        final List<FlowContext> contexts = flowDefinition.addFlows(this, flows);


        //camel uses a Services design. we aren't using services right now, so just loop over flows and start them
        for (Flow flow : flows) {
            final List<Service> services = flow.getServices();
            flow.onStartingServices(services);
        }

        flowContexts.addAll(contexts);
    }

    @Override
    public ServerFactory getServerFactory() {
        return defaultServerFactory;
    }

    @Override
    public ClientFactory getClientFactory() {
        return defaultClientFactory;
    }

    @Override
    public Endpoint getEndpoint(String uri, JsonObject configOverride) {

        final String[] splitUri = uri.split(":");
        if (splitUri.length < 2) {
            throw new ResolveEndpointFailedException("Invalid uri");
        }

        final String scheme = splitUri[0];
        Component component = getComponent(scheme);
        try {
            return component.createEndpoint(uri, configOverride);
        } catch (Exception e) {
            throw new ResolveEndpointFailedException("Error creating endpoint", e);
        }

    }

    @Override
    public Component getComponent(String scheme) {

        Component component = components.get(scheme);
        if (component == null) {
            try {

                final String componentClassName = String.format("com.nxttxn.vramel.components.%s.%sComponent", scheme, WordUtils.capitalize(scheme));
                final Class<?> aClass = getClass().getClassLoader().loadClass(componentClassName);
                final Constructor<?> constructor = aClass.getConstructor(VramelContext.class);
                component = (Component) constructor.newInstance(this);
                addComponent(scheme, component);
            } catch (Exception e) {
                throw new ResolveEndpointFailedException(String.format("Unknown component, %s", scheme), e);
            }
        }
        return component;

    }

    @Override
    public <T extends Component> T getComponent(String name, Class<T> klass) {
        Component component = getComponent(name);
        if (klass.isInstance(component)) {
            return klass.cast(component);
        } else {
            String message;
            if (component == null) {
                message = "Did not find component given by the name: " + name;
            } else {
                message = "Found component of type: " + component.getClass() + " instead of expected: " + klass;
            }
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public void addComponent(String componentName, Component component) {
        ObjectHelper.notNull(component, "component");
        synchronized (components) {
            if (components.containsKey(componentName)) {
                throw new IllegalArgumentException("Cannot add component as its already previously added: " + componentName);
            }
            component.setVramelContext(this);
            components.put(componentName, component);


            // keep reference to properties component up to date
            if (component instanceof PropertiesComponent && "properties".equals(componentName)) {
                propertiesComponent = (PropertiesComponent) component;
            }
        }
    }

    @Override
    public Endpoint getEndpoint(String uri) {
        //eventually we'll need a nicer way to allow the dynamic router to specify a config
        return getEndpoint(uri, getDefaultEndpointConfig());
    }

    @Override
    public JsonObject getDefaultEndpointConfig() {
        return getConfig().getObject(defaultEndpointConfig, new JsonObject());
    }

    @Override
    public Vertx getVertx() {
        return busModBase.getVertx();
    }
    protected PropertiesComponent getPropertiesComponent() {
        return propertiesComponent;
    }

    @Override
    public String resolvePropertyPlaceholders(String text) throws Exception {
        // While it is more efficient to only do the lookup if we are sure we need the component,
        // with custom tokens, we cannot know if the URI contains a property or not without having
        // the component.  We also lose fail-fast behavior for the missing component with this change.
        PropertiesComponent pc = getPropertiesComponent();

        // Do not parse uris that are designated for the properties component as it will handle that itself
        if (text != null && !text.startsWith("properties:")) {
            // No component, assume default tokens.
            if (pc == null && text.contains(PropertiesComponent.DEFAULT_PREFIX_TOKEN)) {
                throw new IllegalArgumentException("PropertiesComponent with name properties must be defined"
                        + " in CamelContext to support property placeholders.");

                // Component available, use actual tokens
            } else if (pc != null && text.contains(pc.getPrefixToken())) {
                // the parser will throw exception if property key was not found
                String answer = pc.parseUri(text);
                logger.debug("Resolved text: {} -> {}", text, answer);
                return answer;
            }
        }

        // return original text as is
        return text;
    }

    @Override
    public String getProperty(String name) {
        String value = getProperties().get(name);
        if (ObjectHelper.isNotEmpty(value)) {
            try {
                value = resolvePropertyPlaceholders(value);
            } catch (Exception ex) {
                // throw CamelRutimeException
                throw new RuntimeVramelException(ex);
            }
        }
        return value;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }


    @Override
    public EventBus getEventBus() {
        return busModBase.getVertx().eventBus();
    }

    @Override
    public JsonObject getConfig() {

        return config;
    }

    @Override
    public Language resolveLanguage(String language) {
        return languages.get(language);
    }

    @Override
    public Boolean isHandleFault() {
        return false;
    }

    public ErrorHandlerBuilder getErrorHandlerBuilder() {
        return (ErrorHandlerBuilder)errorHandlerBuilder;
    }

    @Override
    public NodeIdFactory getNodeIdFactory() {
        return nodeIdFactory;
    }

    @Override
    public UuidGenerator getUuidGenerator() {
        return uuidGenerator;
    }

    public void setErrorHandlerBuilder(ErrorHandlerFactory errorHandlerBuilder) {
        this.errorHandlerBuilder = errorHandlerBuilder;
    }

    @Override
    public TypeConverter getTypeConverter() {
        return new TypeConverter() {
            @Override
            public <T> T convertTo(Class<T> type, Object value) throws TypeConversionException {
                return (T) value;
            }

            @Override
            public <T> T convertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException {
                return (T) value;
            }

            @Override
            public <T> T mandatoryConvertTo(Class<T> type, Object value) throws TypeConversionException, NoTypeConversionAvailableException {
                return (T) value;
            }

            @Override
            public <T> T mandatoryConvertTo(Class<T> type, Exchange exchange, Object value) throws TypeConversionException, NoTypeConversionAvailableException {
                return (T) value;
            }

            @Override
            public <T> T tryConvertTo(Class<T> type, Object value) {
                return (T) value;
            }

            @Override
            public <T> T tryConvertTo(Class<T> type, Exchange exchange, Object value) {
                return (T) value;
            }
        };
    }

    @Override
    public Registry getRegistry() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassResolver getClassResolver() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Injector getInjector() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
