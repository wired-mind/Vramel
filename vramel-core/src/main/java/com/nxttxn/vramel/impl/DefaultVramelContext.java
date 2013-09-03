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
import com.nxttxn.vramel.language.property.PropertyLanguage;
import com.nxttxn.vramel.language.simple.SimpleLanguage;
import com.nxttxn.vramel.model.DataFormatDefinition;
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
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.*;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultVramelContext extends ServiceSupport implements ModelVramelContext {
    private final Logger log = LoggerFactory.getLogger(DefaultVramelContext.class);
    private final Vertx vertx;
    private VramelContextNameStrategy nameStrategy = new DefaultVramelContextNameStrategy();
    private final String defaultEndpointConfig = "default_endpoint_config";
    private Map<EndpointKey, Endpoint> endpoints;
    private final AtomicInteger endpointKeyCounter = new AtomicInteger();

    private volatile boolean firstStartDone;
    private volatile boolean doNotStartFlowsOnFirstStart;
    private final ThreadLocal<Boolean> isStartingRoutes = new ThreadLocal<Boolean>();
    private Boolean autoStartup = Boolean.TRUE;

    private List<FlowDefinition> flowDefinitions = Lists.newArrayList();
    private DefaultServerFactory defaultServerFactory;
    private ClientFactory defaultClientFactory;
    private List<Consumer> consumers = Lists.newArrayList();
    private Boolean lazyLoadTypeConverters = Boolean.FALSE;
    private Map<String, Component> components = Maps.newHashMap();
    private Map<String, DataFormatDefinition> dataFormats = new HashMap<String, DataFormatDefinition>();
    private DataFormatResolver dataFormatResolver = new DefaultDataFormatResolver();

    private final Map<String, FactoryFinder> factories = new HashMap<String, FactoryFinder>();
    private final Map<String, FlowService> routeServices = new LinkedHashMap<String, FlowService>();
    private final Map<String, FlowService> suspendedRouteServices = new LinkedHashMap<String, FlowService>();
    private LanguageResolver languageResolver = null;
    private ClassResolver classResolver = new DefaultClassResolver();
    private PackageScanClassResolver packageScanClassResolver;
    private FactoryFinderResolver factoryFinderResolver = new DefaultFactoryFinderResolver();
    private FactoryFinder defaultFactoryFinder;
    private Injector injector;
    private TypeConverter typeConverter;
    private TypeConverterRegistry typeConverterRegistry;
    private JsonObject config = new JsonObject();
    private final HashMap<String, Language> languages = new HashMap<String, Language>() {{
        put("bean", new BeanLanguage());
        put("simple", new SimpleLanguage());
        put("property", new PropertyLanguage());
    }};
    private ErrorHandlerFactory errorHandlerBuilder;
    private List<FlowContext> flowContexts = Lists.newArrayList();
    private NodeIdFactory nodeIdFactory = new DefaultNodeIdFactory();
    private UuidGenerator uuidGenerator = createDefaultUuidGenerator();
    private Map<String, String> properties = new HashMap<String, String>();
    private PropertiesComponent propertiesComponent;
    private ShutdownStrategy shutdownStrategy = null;
    private final Set<Flow> flows = new LinkedHashSet<Flow>();
    private final List<Service> servicesToClose = new ArrayList<Service>();
    private final List<RouteStartupOrder> routeStartupOrder = new ArrayList<RouteStartupOrder>();
    // start auto assigning route ids using numbering 1000 and upwards
    private int defaultRouteStartupOrder = 1000;
    private final Set<StartupListener> startupListeners = new LinkedHashSet<StartupListener>();
    private final StopWatch stopWatch = new StopWatch(false);
    private Date startDate;

    private UuidGenerator createDefaultUuidGenerator() {
        return new JavaUuidGenerator();
    }

    public DefaultVramelContext(Vertx vertx) {
        this.vertx = vertx;
        this.defaultServerFactory = new DefaultServerFactory(vertx);
        this.defaultClientFactory = new DefaultClientFactory(vertx);

        this.endpoints = new EndpointRegistry(this);

        packageScanClassResolver = new DefaultPackageScanClassResolver();
    }

    public DefaultVramelContext(BusModBase busModBase) {
        this(busModBase.getVertx());

        this.config = busModBase.getContainer().getConfig();
    }


    @Override
    public void addFlowBuilder(FlowsBuilder flowsBuilder) throws Exception {
        flowsBuilder.addFlowsToVramelContext(this);
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

    private void startFlow(FlowDefinition flow) throws Exception {

        // assign ids to the routes and validate that the id's is all unique
        FlowDefinitionHelper.forceAssignIds(this, flowDefinitions);
        String duplicate = FlowDefinitionHelper.validateUniqueIds(flow, flowDefinitions);
        if (duplicate != null) {
            throw new FailedToStartFlowException(flow.getId(), "duplicate id detected: " + duplicate + ". Please correct ids to be unique among all your routes.");
        }

        // indicate we are staring the route using this thread so
        // we are able to query this if needed
        isStartingRoutes.set(true);
        try {
            // must ensure route is prepared, before we can start it
            flow.prepare(this);

            List<Flow> flows = new ArrayList<Flow>();
            List<FlowContext> flowContexts = flow.addFlows(this, flows);
            FlowService flowService = new FlowService(this, flow, flowContexts, flows);
            startRouteService(flowService, true);
        } finally {
            // we are done staring routes
            isStartingRoutes.remove();
        }
    }

    public boolean isStartingRoutes() {
        Boolean answer = isStartingRoutes.get();
        return answer != null && answer;
    }

    /**
     * Starts the given route service
     */
    protected synchronized void startRouteService(FlowService flowService, boolean addingRoutes) throws Exception {
        // we may already be starting routes so remember this, so we can unset accordingly in finally block
        boolean alreadyStartingRoutes = isStartingRoutes();
        if (!alreadyStartingRoutes) {
            isStartingRoutes.set(true);
        }

        try {
            // the route service could have been suspended, and if so then resume it instead
            if (flowService.getStatus().isSuspended()) {
                resumeRouteService(flowService);
            } else {
                // start the route service
                routeServices.put(flowService.getId(), flowService);
                if (shouldStartRoutes()) {
                    // this method will log the routes being started
                    safelyStartRouteServices(true, true, true, false, addingRoutes, flowService);
                    // start route services if it was configured to auto startup and we are not adding routes
                    boolean autoStartup = flowService.getFlowDefinition().isAutoStartup(this);
                    if (!addingRoutes || autoStartup) {
                        // start the route since auto start is enabled or we are starting a route (not adding new routes)
                        flowService.start();
                    }
                }
            }
        } finally {
            if (!alreadyStartingRoutes) {
                isStartingRoutes.remove();
            }
        }
    }

    @Override
    public ProducerTemplate createProducerTemplate() {
        int size = VramelContextHelper.getMaximumCachePoolSize(this);
        return createProducerTemplate(size);
    }

    @Override
    public ProducerTemplate createProducerTemplate(int maximumCacheSize) {
        DefaultProducerTemplate answer = new DefaultProducerTemplate(this);
        answer.setMaximumCacheSize(maximumCacheSize);
        // start it so its ready to use
        try {
            startService(answer);
        } catch (Exception e) {
            throw ObjectHelper.wrapRuntimeCamelException(e);
        }
        return answer;
    }
    /**
     * Should we start newly added routes?
     */
    protected boolean shouldStartRoutes() {
        return isStarted() && !isStarting();
    }

    /**
     * @see #safelyStartRouteServices(boolean,boolean,boolean,boolean,java.util.Collection)
     */
    protected synchronized void safelyStartRouteServices(boolean forceAutoStart, boolean checkClash, boolean startConsumer,
                                                         boolean resumeConsumer, boolean addingRoutes, FlowService... routeServices) throws Exception {
        safelyStartRouteServices(checkClash, startConsumer, resumeConsumer, addingRoutes, Arrays.asList(routeServices));
    }




    private DefaultFlowStartupOrder doPrepareRouteToBeStarted(FlowService flowService) {
        // add the inputs from this flow service to the list to start afterwards
        // should be ordered according to the startup number
        Integer startupOrder = flowService.getFlowDefinition().getStartupOrder();
        if (startupOrder == null) {
            // auto assign a default startup order
            startupOrder = defaultRouteStartupOrder++;
        }

        // create holder object that contains information about this flow to be started
        Flow flow = flowService.getFlows().iterator().next();
        return new DefaultFlowStartupOrder(startupOrder, flow, flowService);
    }
    /**
     * Starts the routes services in a proper manner which ensures the routes will be started in correct order,
     * check for clash and that the routes will also be shutdown in correct order as well.
     * <p/>
     * This method <b>must</b> be used to start routes in a safe manner.
     *
     * @param checkClash     whether to check for startup order clash
     * @param startConsumer  whether the route consumer should be started. Can be used to warmup the route without starting the consumer.
     * @param resumeConsumer whether the route consumer should be resumed.
     * @param addingRoutes   whether we are adding new routes
     * @param flowServices  the routes
     * @throws Exception is thrown if error starting the routes
     */
    protected synchronized void safelyStartRouteServices(boolean checkClash, boolean startConsumer, boolean resumeConsumer,
                                                         boolean addingRoutes, Collection<FlowService> flowServices) throws Exception {
        // list of inputs to start when all the routes have been prepared for starting
        // we use a tree map so the routes will be ordered according to startup order defined on the route
        Map<Integer, DefaultFlowStartupOrder> inputs = new TreeMap<Integer, DefaultFlowStartupOrder>();

        // figure out the order in which the routes should be started
        for (FlowService flowService : flowServices) {
            DefaultFlowStartupOrder order = doPrepareRouteToBeStarted(flowService);
            // check for clash before we add it as input
            if (checkClash) {
                doCheckStartupOrderClash(order, inputs);
            }
            inputs.put(order.getStartupOrder(), order);
        }

        // warm up routes before we start them
        doWarmUpFlows(inputs, startConsumer);

        if (startConsumer) {
            if (resumeConsumer) {
                // and now resume the routes
                doResumeRouteConsumers(inputs, addingRoutes);
            } else {
                // and now start the routes
                // and check for clash with multiple consumers of the same endpoints which is not allowed
                doStartRouteConsumers(inputs, addingRoutes);
            }
        }

        // inputs no longer needed
        inputs.clear();
    }

    private void doStartRouteConsumers(Map<Integer, DefaultFlowStartupOrder> inputs, boolean addingRoutes) throws Exception {
        doStartOrResumeRouteConsumers(inputs, false, addingRoutes);
    }
    private void doResumeRouteConsumers(Map<Integer, DefaultFlowStartupOrder> inputs, boolean addingRoutes) throws Exception {
        doStartOrResumeRouteConsumers(inputs, true, addingRoutes);
    }


    private boolean doCheckMultipleConsumerSupportClash(Endpoint endpoint, List<Endpoint> routeInputs) {
        // is multiple consumers supported
        boolean multipleConsumersSupported = false;
        if (endpoint instanceof MultipleConsumersSupport) {
            multipleConsumersSupported = ((MultipleConsumersSupport) endpoint).isMultipleConsumersSupported();
        }

        if (multipleConsumersSupported) {
            // multiple consumer allowed, so return true
            return true;
        }

        // check in progress list
        if (routeInputs.contains(endpoint)) {
            return false;
        }

        return true;
    }


    private boolean doCheckStartupOrderClash(DefaultFlowStartupOrder answer, Map<Integer, DefaultFlowStartupOrder> inputs) throws FailedToStartFlowException {
        // check for clash by startupOrder id
        DefaultFlowStartupOrder other = inputs.get(answer.getStartupOrder());
        if (other != null && answer != other) {
            String otherId = other.getFlow().getId();
            throw new FailedToStartFlowException(answer.getFlow().getId(), "startupOrder clash. Route " + otherId + " already has startupOrder "
                    + answer.getStartupOrder() + " configured which this route have as well. Please correct startupOrder to be unique among all your routes.");
        }
        // check in existing already started as well
        for (RouteStartupOrder order : routeStartupOrder) {
            String otherId = order.getFlow().getId();
            if (answer.getFlow().getId().equals(otherId)) {
                // its the same route id so skip clash check as its the same route (can happen when using suspend/resume)
            } else if (answer.getStartupOrder() == order.getStartupOrder()) {
                throw new FailedToStartFlowException(answer.getFlow().getId(), "startupOrder clash. Route " + otherId + " already has startupOrder "
                        + answer.getStartupOrder() + " configured which this route have as well. Please correct startupOrder to be unique among all your routes.");
            }
        }
        return true;
    }

    private void doStartOrResumeRouteConsumers(Map<Integer, DefaultFlowStartupOrder> inputs, boolean resumeOnly, boolean addingRoute) throws Exception {
        List<Endpoint> routeInputs = new ArrayList<Endpoint>();

        for (Map.Entry<Integer, DefaultFlowStartupOrder> entry : inputs.entrySet()) {
            Integer order = entry.getKey();
            Flow flow = entry.getValue().getFlow();
            FlowService flowService = entry.getValue().getFlowService();

            // if we are starting camel, then skip routes which are configured to not be auto started
            boolean autoStartup = flowService.getFlowDefinition().isAutoStartup(this);
            if (addingRoute && !autoStartup) {
                log.info("Skipping starting of flow " + flowService.getId() + " as its configured with autoStartup=false");
                continue;
            }

            // start the service
            for (Consumer consumer : flowService.getInputs().values()) {
                Endpoint endpoint = consumer.getEndpoint();

                // check multiple consumer violation, with the other routes to be started
                if (!doCheckMultipleConsumerSupportClash(endpoint, routeInputs)) {
                    throw new FailedToStartFlowException(flowService.getId(),
                            "Multiple consumers for the same endpoint is not allowed: " + endpoint);
                }

                // check for multiple consumer violations with existing routes which
                // have already been started, or is currently starting
                List<Endpoint> existingEndpoints = new ArrayList<Endpoint>();
                for (Flow existingRoute : getFlows()) {
                    if (flow.getId().equals(existingRoute.getId())) {
                        // skip ourselves
                        continue;
                    }
                    Endpoint existing = existingRoute.getEndpoint();
                    ServiceStatus status = getRouteStatus(existingRoute.getId());
                    if (status != null && (status.isStarted() || status.isStarting())) {
                        existingEndpoints.add(existing);
                    }
                }
                if (!doCheckMultipleConsumerSupportClash(endpoint, existingEndpoints)) {
                    throw new FailedToStartFlowException(flowService.getId(),
                            "Multiple consumers for the same endpoint is not allowed: " + endpoint);
                }

                // start the consumer on the flow
                log.debug("Route: {} >>> {}", flow.getId(), flow);
                if (resumeOnly) {
                    log.debug("Resuming consumer (order: {}) on flow: {}", order, flow.getId());
                } else {
                    log.debug("Starting consumer (order: {}) on flow: {}", order, flow.getId());
                }

                if (resumeOnly && flow.supportsSuspension()) {
                    // if we are resuming and the flow can be resumed
                    ServiceHelper.resumeService(consumer);
                    log.info("Route: " + flow.getId() + " resumed and consuming from: " + endpoint);
                } else {
//                    // when starting we should invoke the lifecycle strategies
//                    for (LifecycleStrategy strategy : lifecycleStrategies) {
//                        strategy.onServiceAdd(this, consumer, flow);
//                    }
                    startService(consumer);
                    log.info("Route: " + flow.getId() + " started and consuming from: " + endpoint);
                }

                routeInputs.add(endpoint);

                // add to the order which they was started, so we know how to stop them in reverse order
                // but only add if we haven't already registered it before (we dont want to double add when restarting)
                boolean found = false;
                for (RouteStartupOrder other : routeStartupOrder) {
                    if (other.getFlow().getId() == flow.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    routeStartupOrder.add(entry.getValue());
                }
            }

            if (resumeOnly) {
                flowService.resume();
            } else {
                // and start the flow service (no need to start children as they are already warmed up)
                flowService.start(false);
            }
        }
    }
    private void doWarmUpFlows(Map<Integer, DefaultFlowStartupOrder> inputs, boolean autoStartup) throws Exception {
        // now prepare the routes by starting its services before we start the input
        for (Map.Entry<Integer, DefaultFlowStartupOrder> entry : inputs.entrySet()) {
            // defer starting inputs till later as we want to prepare the routes by starting
            // all their processors and child services etc.
            // then later we open the floods to Camel by starting the inputs
            // what this does is to ensure Camel is more robust on starting routes as all routes
            // will then be prepared in time before we start inputs which will consume messages to be routed
            FlowService flowService = entry.getValue().getFlowService();
            log.debug("Warming up flow id: {} having autoStartup={}", flowService.getId(), autoStartup);
            flowService.warmUp();
        }
    }

    /**
     * Resumes the given route service
     */
    protected synchronized void resumeRouteService(FlowService routeService) throws Exception {
        // the route service could have been stopped, and if so then start it instead
        if (!routeService.getStatus().isSuspended()) {
            startRouteService(routeService, false);
        } else {
            // resume the route service
            if (shouldStartRoutes()) {
                // this method will log the routes being started
                safelyStartRouteServices(true, false, true, true, false, routeService);
                // must resume route service as well
                routeService.resume();
            }
        }
    }

    public String getPropertyPrefixToken() {
        PropertiesComponent pc = getPropertiesComponent();

        if (pc != null) {
            return pc.getPrefixToken();
        } else {
            return null;
        }
    }

    public String getPropertySuffixToken() {
        PropertiesComponent pc = getPropertiesComponent();

        if (pc != null) {
            return pc.getSuffixToken();
        } else {
            return null;
        }
    }

    protected void logRouteState(Flow flow, String state) {
        if (log.isInfoEnabled()) {
            if (flow.getConsumer() != null) {
                log.info("Route: {} is {}, was consuming from: {}", new Object[]{flow.getId(), state, flow.getConsumer().getEndpoint()});
            } else {
                log.info("Route: {} is {}.", flow.getId(), state);
            }
        }
    }


    protected synchronized void stopRouteService(FlowService flowService, boolean removingRoutes) throws Exception {
        flowService.setRemovingRoutes(removingRoutes);
        stopRouteService(flowService);
    }

    protected synchronized void stopRouteService(FlowService flowService) throws Exception {
        flowService.stop();
        for (Flow flow : flowService.getFlows()) {
            logRouteState(flow, "stopped");
        }
    }

    public void removeFlowCollection(Collection<Flow> flows) {
        this.flows.removeAll(flows);
    }
    public void addFlowCollection(List<Flow> flows) {
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

    /**
     * Normalize uri so we can do endpoint hits with minor mistakes and parameters is not in the same order.
     *
     * @param uri the uri
     * @return normalized uri
     * @throws ResolveEndpointFailedException if uri cannot be normalized
     */
    protected static String normalizeEndpointUri(String uri) {
        try {
            uri = URISupport.normalizeUri(uri);
        } catch (Exception e) {
            throw new ResolveEndpointFailedException(uri, e);
        }
        return uri;
    }


    /**
     * Gets the endpoint key to use for lookup or whe adding endpoints to the {@link EndpointRegistry}
     *
     * @param uri the endpoint uri
     * @return the key
     */
    protected EndpointKey getEndpointKey(String uri) {
        return new EndpointKey(uri);
    }

    /**
     * Gets the endpoint key to use for lookup or whe adding endpoints to the {@link EndpointRegistry}
     *
     * @param uri      the endpoint uri
     * @param endpoint the endpoint
     * @return the key
     */
    protected EndpointKey getEndpointKey(String uri, Endpoint endpoint) {
        if (endpoint != null && !endpoint.isSingleton()) {
            int counter = endpointKeyCounter.incrementAndGet();
            return new EndpointKey(uri + ":" + counter);
        } else {
            return new EndpointKey(uri);
        }
    }

    // Endpoint Management Methods
    // -----------------------------------------------------------------------

    public Collection<Endpoint> getEndpoints() {
        return new ArrayList<Endpoint>(endpoints.values());
    }

    public Map<String, Endpoint> getEndpointMap() {
        TreeMap<String, Endpoint> answer = new TreeMap<String, Endpoint>();
        for (Map.Entry<EndpointKey, Endpoint> entry : endpoints.entrySet()) {
            answer.put(entry.getKey().get(), entry.getValue());
        }
        return answer;
    }

    public Endpoint hasEndpoint(String uri) {
        return endpoints.get(getEndpointKey(uri));
    }

    public Endpoint addEndpoint(String uri, Endpoint endpoint) throws Exception {
        Endpoint oldEndpoint;

        startService(endpoint);
        oldEndpoint = endpoints.remove(getEndpointKey(uri));
//        for (LifecycleStrategy strategy : lifecycleStrategies) {
//            strategy.onEndpointAdd(endpoint);
//        }
        addEndpointToRegistry(uri, endpoint);
        if (oldEndpoint != null) {
            stopServices(oldEndpoint);
        }

        return oldEndpoint;
    }

    private void stopServices(Object service) throws Exception {
        // allow us to do custom work before delegating to service helper
        try {
            ServiceHelper.stopService(service);
        } catch (Exception e) {
            // fire event
//            EventHelper.notifyServiceStopFailure(this, service, e);
            // rethrow to signal error with stopping
            throw e;
        }
    }

    /**
     * Strategy to add the given endpoint to the internal endpoint registry
     *
     * @param uri      uri of the endpoint
     * @param endpoint the endpoint to add
     * @return the added endpoint
     */
    protected Endpoint addEndpointToRegistry(String uri, Endpoint endpoint) {
        ObjectHelper.notEmpty(uri, "uri");
        ObjectHelper.notNull(endpoint, "endpoint");

        // if there is endpoint strategies, then use the endpoints they return
        // as this allows to intercept endpoints etc.
//        for (EndpointStrategy strategy : endpointStrategies) {
//            endpoint = strategy.registerEndpoint(uri, endpoint);
//        }
        endpoints.put(getEndpointKey(uri, endpoint), endpoint);
        return endpoint;
    }

    public Collection<Endpoint> removeEndpoints(String uri) throws Exception {
        Collection<Endpoint> answer = new ArrayList<Endpoint>();
        Endpoint oldEndpoint = endpoints.remove(getEndpointKey(uri));
        if (oldEndpoint != null) {
            answer.add(oldEndpoint);
            stopServices(oldEndpoint);
        } else {
            for (Map.Entry<EndpointKey, Endpoint> entry : endpoints.entrySet()) {
                oldEndpoint = entry.getValue();
                if (EndpointHelper.matchEndpoint(this, oldEndpoint.getEndpointUri(), uri)) {
                    try {
                        stopServices(oldEndpoint);
                    } catch (Exception e) {
                        log.warn("Error stopping endpoint " + oldEndpoint + ". This exception will be ignored.", e);
                    }
                    answer.add(oldEndpoint);
                    endpoints.remove(entry.getKey());
                }
            }
        }

//        // notify lifecycle its being removed
//        for (Endpoint endpoint : answer) {
//            for (LifecycleStrategy strategy : lifecycleStrategies) {
//                strategy.onEndpointRemove(endpoint);
//            }
//        }

        return answer;
    }


    @Override
    public Endpoint getEndpoint(String uri) {
        return getEndpoint(uri, getConfig().getObject(defaultEndpointConfig, new JsonObject()));
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



    public Endpoint getEndpoint(String uri, JsonObject configOverride) {
        ObjectHelper.notEmpty(uri, "uri");
        ObjectHelper.notNull(configOverride, "configOverride");

        log.trace("Getting endpoint with uri: {}", uri);

        // in case path has property placeholders then try to let property component resolve those
        try {
            uri = resolvePropertyPlaceholders(uri);
        } catch (Exception e) {
            throw new ResolveEndpointFailedException(uri, e);
        }

        final String rawUri = uri;

        // normalize uri so we can do endpoint hits with minor mistakes and parameters is not in the same order
        uri = normalizeEndpointUri(uri);

        log.trace("Getting endpoint with raw uri: {}, normalized uri: {}", rawUri, uri);

        Endpoint answer;
        String scheme = null;
        EndpointKey key = getEndpointKey(uri);
        answer = endpoints.get(key);
        if (answer == null) {
            try {
                // Use the URI prefix to find the component.
                String splitURI[] = ObjectHelper.splitOnCharacter(uri, ":", 2);
                if (splitURI[1] != null) {
                    scheme = splitURI[0];
                    log.trace("Endpoint uri: {} is from component with name: {}", uri, scheme);
                    Component component = getComponent(scheme);

                    // Ask the component to resolve the endpoint.
                    if (component != null) {
                        log.trace("Creating endpoint from uri: {} using component: {}", uri, component);

                        // Have the component create the endpoint if it can.
                        if (component.useRawUri()) {
                            answer = component.createEndpoint(rawUri, configOverride);
                        } else {
                            answer = component.createEndpoint(uri, configOverride);
                        }

                        if (answer != null && log.isDebugEnabled()) {
                            log.debug("{} converted to endpoint: {} by component: {}", new Object[]{URISupport.sanitizeUri(uri), answer, component});
                        }
                    }
                }

                if (answer == null) {
                    // no component then try in registry and elsewhere
                    answer = createEndpoint(uri);
                    log.trace("No component to create endpoint from uri: {} fallback lookup in registry -> {}", uri, answer);
                }

                if (answer != null) {
                    addService(answer);
                    answer = addEndpointToRegistry(uri, answer);
                }
            } catch (Exception e) {
                throw new ResolveEndpointFailedException(uri, e);
            }
        }

        // unknown scheme
        if (answer == null && scheme != null) {
            throw new ResolveEndpointFailedException(uri, "No component found with scheme: " + scheme);
        }

        return answer;
    }

    /**
     * A pluggable strategy to allow an endpoint to be created without requiring
     * a component to be its factory, such as for looking up the URI inside some
     * {@link Registry}
     *
     * @param uri the uri for the endpoint to be created
     * @return the newly created endpoint or null if it could not be resolved
     */
    protected Endpoint createEndpoint(String uri) {
        Object value = getRegistry().lookupByName(uri);
        if (value instanceof Endpoint) {
            return (Endpoint) value;
        } else if (value instanceof Processor) {
            return new ProcessorEndpoint(uri, this, (Processor) value);
        } else if (value != null) {
            return convertBeanToEndpoint(uri, value);
        }
        return null;
    }

    /**
     * Strategy method for attempting to convert the bean from a {@link Registry} to an endpoint using
     * some kind of transformation or wrapper
     *
     * @param uri  the uri for the endpoint (and name in the registry)
     * @param bean the bean to be converted to an endpoint, which will be not null
     * @return a new endpoint
     */
    protected Endpoint convertBeanToEndpoint(String uri, Object bean) {
        throw new IllegalArgumentException("uri: " + uri + " bean: " + bean
                + " could not be converted to an Endpoint");
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
    public JsonObject getDefaultEndpointConfig() {
        return getConfig().getObject(defaultEndpointConfig, new JsonObject());
    }

    @Override
    public Vertx getVertx() {
        return vertx;
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
                log.debug("Resolved text: {} -> {}", text, answer);
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
        return getVertx().eventBus();
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

    public boolean removeService(Object object) throws Exception {
        if (object instanceof Service) {
            Service service = (Service) object;

//            for (LifecycleStrategy strategy : lifecycleStrategies) {
//                if (service instanceof Endpoint) {
//                    // use specialized endpoint remove
//                    strategy.onEndpointRemove((Endpoint) service);
//                } else {
//                    strategy.onServiceRemove(this, service, null);
//                }
//            }
            return servicesToClose.remove(service);
        }
        return false;
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
            boolean singleton = true; // assume singleton by default
            if (service instanceof IsSingleton) {
                singleton = ((IsSingleton) service).isSingleton();
            }
            // do not add endpoints as they have their own list
            if (singleton && !(service instanceof Endpoint)) {
                // only add to list of services to close if its not already there
                if (closeOnShutdown && !hasService(service)) {
                    servicesToClose.add(service);
                }
            }
        }

        // and then ensure service is started (as stated in the javadoc)
        if (object instanceof Service) {
            startService((Service)object);
        } else if (object instanceof Collection<?>) {
            startServices((Collection<?>)object);
        }
    }

    public boolean hasService(Object object) {
        if (object instanceof Service) {
            Service service = (Service) object;
            return servicesToClose.contains(service);
        }
        return false;
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


    public void setDataFormats(Map<String, DataFormatDefinition> dataFormats) {
        this.dataFormats = dataFormats;
    }

    public Map<String, DataFormatDefinition> getDataFormats() {
        return dataFormats;
    }

    public DataFormatResolver getDataFormatResolver() {
        return dataFormatResolver;
    }

    public void setDataFormatResolver(DataFormatResolver dataFormatResolver) {
        this.dataFormatResolver = dataFormatResolver;
    }

    public DataFormat resolveDataFormat(String name) {
        DataFormat answer = dataFormatResolver.resolveDataFormat(name, this);

        // inject CamelContext if aware
        if (answer != null && answer instanceof VramelContextAware) {
            ((VramelContextAware) answer).setVramelContext(this);
        }

        return answer;
    }

    public DataFormatDefinition resolveDataFormatDefinition(String name) {
        // lookup type and create the data format from it
        DataFormatDefinition type = lookup(this, name, DataFormatDefinition.class);
        if (type == null && getDataFormats() != null) {
            type = getDataFormats().get(name);
        }
        return type;
    }


    private static <T> T lookup(VramelContext context, String ref, Class<T> type) {
        try {
            return context.getRegistry().lookupByNameAndType(ref, type);
        } catch (Exception e) {
            // need to ignore not same type and return it as null
            return null;
        }
    }

    public Component hasComponent(String componentName) {
        return components.get(componentName);
    }

    public ShutdownStrategy getShutdownStrategy() {
        return shutdownStrategy;
    }

    public void setShutdownStrategy(ShutdownStrategy shutdownStrategy) {
        this.shutdownStrategy = shutdownStrategy;
    }

    public Boolean isAutoStartup() {
        return autoStartup != null && autoStartup;
    }

    public ServiceStatus getRouteStatus(String key) {
        FlowService flowService = routeServices.get(key);
        if (flowService != null) {
            return flowService.getStatus();
        }
        return null;
    }
    public void start() throws Exception {
        startDate = new Date();
        stopWatch.restart();
        log.info("Vramel " + getVersion() + " (VramelContext: " + getName() + ") is starting");

        doNotStartFlowsOnFirstStart = !firstStartDone && !isAutoStartup();

        // if the context was configured with auto startup = false, and we are already started,
        // then we may need to start the routes on the 2nd start call
        if (firstStartDone && !isAutoStartup() && isStarted()) {
            // invoke this logic to warm up the routes and if possible also start the routes
            doStartOrResumeRoutes(routeServices, true, true, false, true);
        }

        // super will invoke doStart which will prepare internal services and start routes etc.
        try {
            firstStartDone = true;
            super.start();
        } catch (VetoCamelContextStartException e) {
            if (e.isRethrowException()) {
                throw e;
            } else {
                log.info("VramelContext ({}) vetoed to not start due {}", getName(), e.getMessage());
                // swallow exception and change state of this camel context to stopped
                stop();
                return;
            }
        }

        stopWatch.stop();
        if (log.isInfoEnabled()) {
            // count how many routes are actually started
            int started = 0;
            for (Flow route : getFlows()) {
                if (getRouteStatus(route.getId()).isStarted()) {
                    started++;
                }
            }
            log.info("Total " + getFlows().size() + " routes, of which " + started + " is started.");
            log.info("Vramel " + getVersion() + " (VramelContext: " + getName() + ") started in " + TimeUtils.printDuration(stopWatch.taken()));
        }
//        EventHelper.notifyCamelContextStarted(this);
    }

    // Implementation methods
    // -----------------------------------------------------------------------

    protected synchronized void doStart() throws Exception {
        try {
            doStartVramel();
            getServerFactory().startAllServers();
        } catch (Exception e) {
            // fire event that we failed to start
//            EventHelper.notifyCamelContextStartupFailed(this, e);
            // rethrow cause
            throw e;
        }
    }

    private void doStartVramel() throws Exception {
//        if (isStreamCaching()) {
//            // only add a new stream cache if not already configured
//            if (StreamCaching.getStreamCaching(this) == null) {
//                log.info("StreamCaching is enabled on CamelContext: " + getName());
//                addInterceptStrategy(new StreamCaching());
//            }
//        }

//        if (isTracing()) {
//            // tracing is added in the DefaultChannel so we can enable it on the fly
//            log.info("Tracing is enabled on CamelContext: " + getName());
//        }
//
//        if (isUseMDCLogging()) {
//            // log if MDC has been enabled
//            log.info("MDC logging is enabled on CamelContext: " + getName());
//        }

//        if (isHandleFault()) {
//            // only add a new handle fault if not already configured
//            if (HandleFault.getHandleFault(this) == null) {
//                log.info("HandleFault is enabled on CamelContext: " + getName());
//                addInterceptStrategy(new HandleFault());
//            }
//        }

//        if (getDelayer() != null && getDelayer() > 0) {
//            // only add a new delayer if not already configured
//            if (Delayer.getDelayer(this) == null) {
//                long millis = getDelayer();
//                log.info("Delayer is enabled with: " + millis + " ms. on CamelContext: " + getName());
//                addInterceptStrategy(new Delayer(millis));
//            }
//        }
//
//        // register debugger
//        if (getDebugger() != null) {
//            log.info("Debugger: " + getDebugger() + " is enabled on CamelContext: " + getName());
//            // register this camel context on the debugger
//            getDebugger().setCamelContext(this);
//            startService(getDebugger());
//            addInterceptStrategy(new Debug(getDebugger()));
//        }
//
//        // start management strategy before lifecycles are started
//        ManagementStrategy managementStrategy = getManagementStrategy();
//        // inject CamelContext if aware
//        if (managementStrategy instanceof CamelContextAware) {
//            ((CamelContextAware) managementStrategy).setCamelContext(this);
//        }
//        ServiceHelper.startService(managementStrategy);
//
//        // start lifecycle strategies
//        ServiceHelper.startServices(lifecycleStrategies);
//        Iterator<LifecycleStrategy> it = lifecycleStrategies.iterator();
//        while (it.hasNext()) {
//            LifecycleStrategy strategy = it.next();
//            try {
//                strategy.onContextStart(this);
//            } catch (VetoCamelContextStartException e) {
//                // okay we should not start Camel since it was vetoed
//                log.warn("Lifecycle strategy vetoed starting CamelContext ({}) due {}", getName(), e.getMessage());
//                throw e;
//            } catch (Exception e) {
//                log.warn("Lifecycle strategy " + strategy + " failed starting CamelContext ({}) due {}", getName(), e.getMessage());
//                throw e;
//            }
//        }
//
//        // start notifiers as services
//        for (EventNotifier notifier : getManagementStrategy().getEventNotifiers()) {
//            if (notifier instanceof Service) {
//                Service service = (Service) notifier;
//                for (LifecycleStrategy strategy : lifecycleStrategies) {
//                    strategy.onServiceAdd(this, service, null);
//                }
//            }
//            if (notifier instanceof Service) {
//                startService((Service)notifier);
//            }
//        }
//
//        // must let some bootstrap service be started before we can notify the starting event
//        EventHelper.notifyCamelContextStarting(this);

        forceLazyInitialization();

        // re-create endpoint registry as the cache size limit may be set after the constructor of this instance was called.
        // and we needed to create endpoints up-front as it may be accessed before this context is started
        endpoints = new EndpointRegistry(this, endpoints);
        addService(endpoints);
        // special for executorServiceManager as want to stop it manually
//        doAddService(executorServiceManager, false);
//        addService(producerServicePool);
//        addService(inflightRepository);
        addService(shutdownStrategy);
        addService(packageScanClassResolver);

        // eager lookup any configured properties component to avoid subsequent lookup attempts which may impact performance
        // due we use properties component for property placeholder resolution at runtime
        Component existing = hasComponent("properties");
        if (existing == null) {
            // no existing properties component so lookup and add as component if possible
            propertiesComponent = getRegistry().lookupByNameAndType("properties", PropertiesComponent.class);
            if (propertiesComponent != null) {
                addComponent("properties", propertiesComponent);
            }
        } else {
            // store reference to the existing properties component
            if (existing instanceof PropertiesComponent) {
                propertiesComponent = (PropertiesComponent) existing;
            } else {
                // properties component must be expected type
                throw new IllegalArgumentException("Found properties component of type: " + existing.getClass() + " instead of expected: " + PropertiesComponent.class);
            }
        }

        // start components
        startServices(components.values());

        // start the route definitions before the routes is started
        startRouteDefinitions(flowDefinitions);

        // start routes
        if (doNotStartFlowsOnFirstStart) {
            log.debug("Skip starting of routes as CamelContext has been configured with autoStartup=false");
        }

        // invoke this logic to warmup the routes and if possible also start the routes
        doStartOrResumeRoutes(routeServices, true, !doNotStartFlowsOnFirstStart, false, true);

        // starting will continue in the start method
    }

    protected void startRouteDefinitions(Collection<FlowDefinition> list) throws Exception {
        if (list != null) {
            for (FlowDefinition flow : list) {
                startFlow(flow);
            }
        }
    }

    /**
     * Force some lazy initialization to occur upfront before we start any
     * components and create routes
     */
    protected void forceLazyInitialization() {
        getRegistry();
        getInjector();
//        getLanguageResolver();
        getTypeConverterRegistry();
        getTypeConverter();

//        if (isTypeConverterStatisticsEnabled() != null) {
//            getTypeConverterRegistry().getStatistics().setStatisticsEnabled(isTypeConverterStatisticsEnabled());
//        }
    }

    /**
     * Starts or resumes the routes
     *
     * @param flowServices  the routes to start (will only start a route if its not already started)
     * @param checkClash     whether to check for startup ordering clash
     * @param startConsumer  whether the route consumer should be started. Can be used to warmup the route without starting the consumer.
     * @param resumeConsumer whether the route consumer should be resumed.
     * @param addingRoutes   whether we are adding new routes
     * @throws Exception is thrown if error starting routes
     */
    protected void doStartOrResumeRoutes(Map<String, FlowService> flowServices, boolean checkClash,
                                         boolean startConsumer, boolean resumeConsumer, boolean addingRoutes) throws Exception {
        // filter out already started routes
        Map<String, FlowService> filtered = new LinkedHashMap<String, FlowService>();
        for (Map.Entry<String, FlowService> entry : flowServices.entrySet()) {
            boolean startable = false;

            Consumer consumer = entry.getValue().getFlows().iterator().next().getConsumer();
            if (consumer instanceof SuspendableService) {
                // consumer could be suspended, which is not reflected in the RouteService status
                startable = ((SuspendableService) consumer).isSuspended();
            }

            if (!startable && consumer instanceof StatefulService) {
                // consumer could be stopped, which is not reflected in the RouteService status
                startable = ((StatefulService) consumer).getStatus().isStartable();
            } else if (!startable) {
                // no consumer so use state from route service
                startable = entry.getValue().getStatus().isStartable();
            }

            if (startable) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        if (!filtered.isEmpty()) {
            // the context is now considered started (i.e. isStarted() == true))
            // starting routes is done after, not during context startup
            safelyStartRouteServices(checkClash, startConsumer, resumeConsumer, addingRoutes, filtered.values());
        }

        // now notify any startup aware listeners as all the routes etc has been started,
        // allowing the listeners to do custom work after routes has been started
        for (StartupListener startup : startupListeners) {
            startup.onVramelContextStarted(this, isStarted());
        }
    }

    public List<RouteStartupOrder> getRouteStartupOrder() {
        return routeStartupOrder;
    }

    private void shutdownServices(Object service) {
        // do not rethrow exception as we want to keep shutting down in case of problems

        // allow us to do custom work before delegating to service helper
        try {
            if (service instanceof Service) {
                ServiceHelper.stopAndShutdownService(service);
            } else if (service instanceof Collection) {
                ServiceHelper.stopAndShutdownServices((Collection<?>)service);
            }
        } catch (Throwable e) {
            log.warn("Error occurred while shutting down service: " + service + ". This exception will be ignored.", e);
            // fire event
//            EventHelper.notifyServiceStopFailure(this, service, e);
        }
    }
    private void shutdownServices(Collection<?> services) {
        // reverse stopping by default
        shutdownServices(services, true);
    }

    private void shutdownServices(Collection<?> services, boolean reverse) {
        Collection<?> list = services;
        if (reverse) {
            List<Object> reverseList = new ArrayList<Object>(services);
            Collections.reverse(reverseList);
            list = reverseList;
        }

        for (Object service : list) {
            shutdownServices(service);
        }
    }
    protected synchronized void doStop() throws Exception {
        stopWatch.restart();
        log.info("Vramel " + getVersion() + " (VramelContext: " + getName() + ") is shutting down");
//        EventHelper.notifyCamelContextStopping(this);

        // stop route inputs in the same order as they was started so we stop the very first inputs first
//        try {
//            // force shutting down routes as they may otherwise cause shutdown to hang
//            shutdownStrategy.shutdownForced(this, getRouteStartupOrder());
//        } catch (Throwable e) {
//            log.warn("Error occurred while shutting down routes. This exception will be ignored.", e);
//        }
        getRouteStartupOrder().clear();

        shutdownServices(routeServices.values());
        // do not clear route services or startup listeners as we can start Camel again and get the route back as before

        // but clear any suspend routes
        suspendedRouteServices.clear();

        // the stop order is important


        // shutdown debugger
//        ServiceHelper.stopAndShutdownService(getDebugger());

        shutdownServices(endpoints.values());
        endpoints.clear();

        shutdownServices(components.values());
        components.clear();

//        try {
//            for (LifecycleStrategy strategy : lifecycleStrategies) {
//                strategy.onContextStop(this);
//            }
//        } catch (Throwable e) {
//            log.warn("Error occurred while stopping lifecycle strategies. This exception will be ignored.", e);
//        }

        // shutdown services as late as possible
        shutdownServices(servicesToClose);
        servicesToClose.clear();

        // must notify that we are stopped before stopping the management strategy
//        EventHelper.notifyCamelContextStopped(this);

        // stop the notifier service
//        for (EventNotifier notifier : getManagementStrategy().getEventNotifiers()) {
//            shutdownServices(notifier);
//        }

        // shutdown executor service and management as the last one
//        shutdownServices(executorServiceManager);
//        shutdownServices(managementStrategy);
//        shutdownServices(managementMBeanAssembler);
//        shutdownServices(lifecycleStrategies);
        // do not clear lifecycleStrategies as we can start Camel again and get the route back as before

        // stop the lazy created so they can be re-created on restart
        forceStopLazyInitialization();

        // stop to clear introspection cache
        IntrospectionSupport.stop();

        stopWatch.stop();
        if (log.isInfoEnabled()) {
            log.info("Uptime {}", getUptime());
            log.info("Apache Camel " + getVersion() + " (CamelContext: " + getName() + ") is shutdown in " + TimeUtils.printDuration(stopWatch.taken()));
        }

        // and clear start date
        startDate = null;
    }

    public String getUptime() {
        // compute and log uptime
        if (startDate == null) {
            return "not started";
        }
        long delta = new Date().getTime() - startDate.getTime();
        return TimeUtils.printDuration(delta);
    }
    /**
     * Force clear lazy initialization so they can be re-created on restart
     */
    protected void forceStopLazyInitialization() {
        injector = null;
        languageResolver = null;
        typeConverterRegistry = null;
        typeConverter = null;
    }

}
