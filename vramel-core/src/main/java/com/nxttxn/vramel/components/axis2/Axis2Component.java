package com.nxttxn.vramel.components.axis2;

import com.google.common.base.Joiner;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ResolveEndpointFailedException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/15/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Axis2Component extends DefaultComponent {



    public Axis2Component(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, JsonObject config) throws Exception {

        final Axis2ChannelAdapter axis2ChannelAdapter = new Axis2ChannelAdapter(getVramelContext(), remaining, config);
        return axis2ChannelAdapter;
    }
}