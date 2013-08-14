package com.nxttxn.vramel.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.components.properties.PropertiesComponent;
import com.nxttxn.vramel.impl.converter.BaseTypeConverterRegistry;
import com.nxttxn.vramel.impl.converter.DefaultTypeConverter;
import com.nxttxn.vramel.impl.converter.LazyLoadingTypeConverter;
import com.nxttxn.vramel.language.bean.BeanLanguage;
import com.nxttxn.vramel.language.simple.SimpleLanguage;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.model.ModelVramelContext;
import com.nxttxn.vramel.spi.*;
import com.nxttxn.vramel.spi.ClassResolver;
import com.nxttxn.vramel.spi.FactoryFinder;
import com.nxttxn.vramel.spi.FactoryFinderResolver;
import com.nxttxn.vramel.spi.Injector;
import com.nxttxn.vramel.spi.Language;
import com.nxttxn.vramel.spi.NodeIdFactory;
import com.nxttxn.vramel.spi.PackageScanClassResolver;
import com.nxttxn.vramel.spi.Registry;
import com.nxttxn.vramel.spi.TypeConverterRegistry;
import com.nxttxn.vramel.spi.UuidGenerator;
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
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultVramelContext implements ModelVramelContext {
    private final Logger logger = LoggerFactory.getLogger(DefaultVramelContext.class);
    private VramelContextNameStrategy nameStrategy = new DefaultVramelContextNameStrategy();
    private final BusModBase busModBase;
    private final String defaultEndpointConfig = "default_endpoint_config";
    private List<FlowDefinition> flowDefinitions = Lists.newArrayList();
    private DefaultServerFactory defaultServerFactory;
    private ClientFactory defaultClientFactory;
    private List<Consumer> consumers = Lists.newArrayList();
    private Boolean lazyLoadTypeConverters = Boolean.FALSE;
    private Map<String, Component> components = Maps.newHashMap();
    private final Map<String, FactoryFinder> factories = new HashMap<String, FactoryFinder>();
    private ClassResolver classResolver = new DefaultClassResolver();
    private PackageScanClassResolver packageScanClassResolver;
    private FactoryFinderResolver factoryFinderResolver = new DefaultFactoryFinderResolver();
    private FactoryFinder defaultFactoryFinder;
    private Injector injector;
    private TypeConverter typeConverter;
    private TypeConverterRegistry typeConverterRegistry;
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
    private final Set<Flow> flows = new LinkedHashSet<Flow>();
    private final Set<StartupListener> startupListeners = new LinkedHashSet<StartupListener>();

    private UuidGenerator createDefaultUuidGenerator() {
        return new JavaUuidGenerator();
    }

    public DefaultVramelContext(BusModBase busModBase) {

        this.busModBase = busModBase;
        this.config = busModBase.getContainer().getConfig();
        this.defaultServerFactory = new DefaultServerFactory(busModBase.getVertx());
        this.defaultClientFactory = new DefaultClientFactory(busModBase.getVertx());


        packageScanClassResolver = new DefaultPackageScanClassResolver();

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
        addFlowCollection(flows);
    }

    private void addFlowCollection(List<Flow> flows) {
        this.flows.addAll(flows);
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

    public <T extends Endpoint> T getEndpoint(String name, Class<T> endpointType) {
        Endpoint endpoint = getEndpoint(name);
        if (endpoint == null) {
            throw new NoSuchEndpointException(name);
        }
//Doesn't exist yet
//        if (endpoint instanceof InterceptSendToEndpoint) {
//            endpoint = ((InterceptSendToEndpoint) endpoint).getDelegate();
//        }
        if (endpointType.isInstance(endpoint)) {
            return endpointType.cast(endpoint);
        } else {
            throw new IllegalArgumentException("The endpoint is not of type: " + endpointType
                    + " but is: " + endpoint.getClass().getCanonicalName());
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
    public List<Flow> getFlows() {
        return new ArrayList<>(flows);
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
        if (typeConverter == null) {
            synchronized (this) {
                // we can synchronize on this as there is only one instance
                // of the camel context (its the container)
                typeConverter = createTypeConverter();
                try {
                    addService(typeConverter);
                } catch (Exception e) {
                    throw ObjectHelper.wrapRuntimeCamelException(e);
                }
            }
        }
        return typeConverter;
    }

    public TypeConverterRegistry getTypeConverterRegistry() {
        if (typeConverterRegistry == null) {
            // init type converter as its lazy
            if (typeConverter == null) {
                getTypeConverter();
            }
            if (typeConverter instanceof TypeConverterRegistry) {
                typeConverterRegistry = (TypeConverterRegistry) typeConverter;
            }
        }
        return typeConverterRegistry;
    }

    public void setTypeConverterRegistry(TypeConverterRegistry typeConverterRegistry) {
        this.typeConverterRegistry = typeConverterRegistry;
    }
    /**
     * Lazily create a default implementation
     */
    protected TypeConverter createTypeConverter() {
        BaseTypeConverterRegistry answer;
        if (isLazyLoadTypeConverters()) {
            answer = new LazyLoadingTypeConverter(packageScanClassResolver, getInjector(), getDefaultFactoryFinder());
        } else {
            answer = new DefaultTypeConverter(packageScanClassResolver, getInjector(), getDefaultFactoryFinder());
        }
        setTypeConverterRegistry(answer);
        return answer;
    }


    @Deprecated
    public Boolean isLazyLoadTypeConverters() {
        return lazyLoadTypeConverters != null && lazyLoadTypeConverters;
    }

    @Deprecated
    public void setLazyLoadTypeConverters(Boolean lazyLoadTypeConverters) {
        this.lazyLoadTypeConverters = lazyLoadTypeConverters;
    }


    public void setTypeConverter(TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public PackageScanClassResolver getPackageScanClassResolver() {
        return packageScanClassResolver;
    }

    public void setPackageScanClassResolver(PackageScanClassResolver packageScanClassResolver) {
        this.packageScanClassResolver = packageScanClassResolver;
    }


    public FactoryFinder getDefaultFactoryFinder() {
        if (defaultFactoryFinder == null) {
            defaultFactoryFinder = factoryFinderResolver.resolveDefaultFactoryFinder(getClassResolver());
        }
        return defaultFactoryFinder;
    }

    public void setFactoryFinderResolver(FactoryFinderResolver resolver) {
        this.factoryFinderResolver = resolver;
    }

    public FactoryFinder getFactoryFinder(String path) throws NoFactoryAvailableException {
        synchronized (factories) {
            FactoryFinder answer = factories.get(path);
            if (answer == null) {
                answer = factoryFinderResolver.resolveFactoryFinder(getClassResolver(), path);
                factories.put(path, answer);
            }
            return answer;
        }
    }
    @Override
    public ClassResolver getClassResolver() {
        return classResolver;
    }

    public void setClassResolver(ClassResolver classResolver) {
        this.classResolver = classResolver;
    }


    @Override
    public Registry getRegistry() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addService(Object object) throws Exception {
        doAddService(object, true);
    }

    @Override
    public ExecutorServiceStrategy getExecutorServiceStrategy() {
        throw new UnsupportedOperationException("Not implemented in vramel");
    }

    private void doAddService(Object object, boolean closeOnShutdown) throws Exception {
        // inject CamelContext
        if (object instanceof VramelContextAware) {
            VramelContextAware aware = (VramelContextAware) object;
            aware.setVramelContext(this);
        }

        if (object instanceof Service) {
            Service service = (Service) object;

            //Haven't implemented services fully. Just enough to startup DefaultTypeConverter for now.
            service.start();

            //Services is hardly implemented at all right now. Just enough to start DefaultTypeConverter
//            for (LifecycleStrategy strategy : lifecycleStrategies) {
//                if (service instanceof Endpoint) {
//                    // use specialized endpoint add
//                    strategy.onEndpointAdd((Endpoint) service);
//                } else {
//                    strategy.onServiceAdd(this, service, null);
//                }
//            }

            // only add to services to close if its a singleton
            // otherwise we could for example end up with a lot of prototype scope endpoints
//            boolean singleton = true; // assume singleton by default
//            if (service instanceof IsSingleton) {
//                singleton = ((IsSingleton) service).isSingleton();
//            }
//            // do not add endpoints as they have their own list
//            if (singleton && !(service instanceof Endpoint)) {
//                // only add to list of services to close if its not already there
//                if (closeOnShutdown && !hasService(service)) {
//                    servicesToClose.add(service);
//                }
//            }
        }

        // and then ensure service is started (as stated in the javadoc)
        if (object instanceof Service) {
            startService((Service)object);
        } else if (object instanceof Collection<?>) {
            startServices((Collection<?>)object);
        }
    }

    private void startService(Service service) throws Exception {
        // and register startup aware so they can be notified when
        // camel context has been started
        if (service instanceof StartupListener) {
            StartupListener listener = (StartupListener) service;
            addStartupListener(listener);
        }

        service.start();
    }

    private void startServices(Collection<?> services) throws Exception {
        for (Object element : services) {
            if (element instanceof Service) {
                startService((Service)element);
            }
        }
    }

    public void addStartupListener(StartupListener listener) throws Exception {
        // either add to listener so we can invoke then later when CamelContext has been started
        // or invoke the callback right now
        if (isStarted()) {
            listener.onVramelContextStarted(this, true);
        } else {
            startupListeners.add(listener);
        }
    }

    //until we fully implemente services. Always started=true
    private boolean isStarted() {
        return true;
    }

    public Injector getInjector() {
        if (injector == null) {
            injector = createInjector();
        }
        return injector;
    }

    @Override
    public void setInjector(Injector injector) {
        this.injector = injector;
    }


    /**
     * Lazily create a default implementation
     */
    protected Injector createInjector() {
        FactoryFinder finder = getDefaultFactoryFinder();
        try {
            return (Injector) finder.newInstance("Injector");
        } catch (NoFactoryAvailableException e) {
            // lets use the default injector
            return new DefaultInjector(this);
        }
    }

    public String getName() {
        return getNameStrategy().getName();
    }

    /**
     * Sets the name of the this context.
     *
     * @param name the name
     */
    public void setName(String name) {
        // use an explicit name strategy since an explicit name was provided to be used
        this.nameStrategy = new ExplicitVramelContextNameStrategy(name);
    }

    public VramelContextNameStrategy getNameStrategy() {
        return nameStrategy;
    }

    public void setNameStrategy(VramelContextNameStrategy nameStrategy) {
        this.nameStrategy = nameStrategy;
    }

}
