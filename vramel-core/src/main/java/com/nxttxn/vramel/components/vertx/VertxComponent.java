package com.nxttxn.vramel.components.vertx;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxComponent extends DefaultComponent {
    public VertxComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final VertxChannelAdapter vertxChannelAdapter = new VertxChannelAdapter(getVramelContext(), remaining, new JsonObject(parameters).copy());
        setProperties(vertxChannelAdapter, parameters);
        parameters.clear();
        return vertxChannelAdapter;
    }
}
