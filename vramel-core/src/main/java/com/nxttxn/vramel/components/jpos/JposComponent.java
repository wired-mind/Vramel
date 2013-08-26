package com.nxttxn.vramel.components.jpos;

import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 5:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class JposComponent extends DefaultComponent {
    public JposComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        final JposChannelAdapter jposChannelAdapter = new JposChannelAdapter(getVramelContext(), new JsonObject(parameters).copy(), remaining);
        parameters.clear();
        return jposChannelAdapter;
    }
}