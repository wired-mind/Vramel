package com.nxttxn.vramel;

import com.nxttxn.vramel.builder.ErrorHandlerBuilder;
import com.nxttxn.vramel.components.properties.PropertiesComponent;
import com.nxttxn.vramel.model.FlowDefinition;
import com.nxttxn.vramel.spi.*;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.json.JsonObject;

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
    void addFlowDefinitions(List<FlowDefinition> flows) throws FailedToCreateRouteException, Exception;

    ServerFactory getServerFactory();

    ClientFactory getClientFactory();

    void addFlowBuilder(FlowsBuilder flowsBuilder) throws Exception;

    void run() throws Exception;

    Endpoint getEndpoint(String uri, JsonObject config);

    Endpoint getEndpoint(String uri);

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
}
