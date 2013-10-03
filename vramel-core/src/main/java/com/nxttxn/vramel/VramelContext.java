package com.nxttxn.vramel;

import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.components.properties.PropertiesComponent;
import com.nxttxn.vramel.model.DataFormatDefinition;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.spi.*;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.Container;

import java.util.List;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 12:30 PM
 * To change this template use File | Settings | File Templates.
 */
public interface VramelContext {

    /**
     * Gets the name (id) of the this context.
     *
     * @return the name
     */
    String getName();

    void addFlowDefinitions(List<FlowDefinition> flows) throws FailedToCreateRouteException, Exception;

    ServerFactory getServerFactory();

    ClientFactory getClientFactory();

    void addFlowBuilder(FlowsBuilder flowsBuilder) throws Exception;

    Endpoint getEndpoint(String uri, JsonObject config);

    Endpoint getEndpoint(String uri);
    /**
     * Resolves the given name to an {@link Endpoint} of the specified type.
     * If the name has a singleton endpoint registered, then the singleton is returned.
     * Otherwise, a new {@link Endpoint} is created and registered.
     *
     * @param name         the name of the endpoint
     * @param endpointType the expected type
     * @return the endpoint
     */
    <T extends Endpoint> T getEndpoint(String name, Class<T> endpointType);

    EventBus getEventBus();

    JsonObject getConfig();

    TypeConverter getTypeConverter();

    Registry getRegistry();

    ClassResolver getClassResolver();

    Injector getInjector();

    <T extends Component> T getComponent(String name, Class<T> klass);

    Component getComponent(String scheme);

    Language resolveLanguage(String language);

    Boolean isHandleFault();

    ErrorHandlerBuilder getErrorHandlerBuilder();

    NodeIdFactory getNodeIdFactory();

    UuidGenerator getUuidGenerator();

    JsonObject getDefaultEndpointConfig();

    Vertx getVertx();

    String resolvePropertyPlaceholders(String text) throws Exception;

    String getProperty(String name);

    Map<String,String> getProperties();

    void addComponent(String componentName, Component component);

    List<Flow> getFlows();

    void setInjector(Injector injector);

    void addService(Object object) throws Exception;

    //Shouldn't be used in vramel
    ExecutorServiceStrategy getExecutorServiceStrategy();

    /**
     * Sets the data formats that can be referenced in the routes.
     *
     * @param dataFormats the data formats
     * @deprecated use {@link org.apache.camel.model.ModelCamelContext#setDataFormats(java.util.Map)}
     */
    @Deprecated
    void setDataFormats(Map<String, DataFormatDefinition> dataFormats);

    /**
     * Gets the data formats that can be referenced in the routes.
     *
     * @return the data formats available
     * @deprecated use {@link org.apache.camel.model.ModelCamelContext#getDataFormats()}
     */
    @Deprecated
    Map<String, DataFormatDefinition> getDataFormats();

    /**
     * Resolve a data format given its name
     *
     * @param name the data format name or a reference to it in the {@link Registry}
     * @return the resolved data format, or <tt>null</tt> if not found
     */
    DataFormat resolveDataFormat(String name);

    /**
     * Resolve a data format definition given its name
     *
     * @param name the data format definition name or a reference to it in the {@link Registry}
     * @return the resolved data format definition, or <tt>null</tt> if not found
     * @deprecated use {@link org.apache.camel.model.ModelCamelContext#resolveDataFormatDefinition(String)}
     */
    @Deprecated
    DataFormatDefinition resolveDataFormatDefinition(String name);

    /**
     * Gets the current data format resolver
     *
     * @return the resolver
     */
    DataFormatResolver getDataFormatResolver();

    /**
     * Sets a custom data format resolver
     *
     * @param dataFormatResolver the resolver
     */
    void setDataFormatResolver(DataFormatResolver dataFormatResolver);

    /**
     * Gets the FactoryFinder which will be used for the loading the factory class from META-INF in the given path
     *
     * @param path the META-INF path
     * @return the factory finder
     * @throws NoFactoryAvailableException is thrown if a factory could not be found
     */
    FactoryFinder getFactoryFinder(String path) throws NoFactoryAvailableException;

    /**
     * Gets the current shutdown strategy
     *
     * @return the strategy
     */
    ShutdownStrategy getShutdownStrategy();

    /**
     * Sets a custom shutdown strategy
     *
     * @param shutdownStrategy the custom strategy
     */
    void setShutdownStrategy(ShutdownStrategy shutdownStrategy);

    /**
     * Returns the configured property placeholder prefix token if and only if the context has
     * property placeholder abilities, otherwise returns {@code null}.
     *
     * @return the prefix token or {@code null}
     */
    String getPropertyPrefixToken();

    /**
     * Returns the configured property placeholder suffix token if and only if the context has
     * property placeholder abilities, otherwise returns {@code null}.
     *
     * @return the suffix token or {@code null}
     */
    String getPropertySuffixToken();


    ProducerTemplate createProducerTemplate();

    ProducerTemplate createProducerTemplate(int maximumCacheSize);

    Container getContainer();
}
