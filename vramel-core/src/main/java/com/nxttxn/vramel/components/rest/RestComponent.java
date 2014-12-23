package com.nxttxn.vramel.components.rest;

import com.google.common.base.Joiner;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.ResolveEndpointFailedException;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultComponent;
import org.vertx.java.core.json.JsonObject;

import java.util.Arrays;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 7:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestComponent extends DefaultComponent {
    public RestComponent(VramelContext vramelContext) {
        super(vramelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        final String separator = ":";
        final String[] splitRemaining = remaining.split(separator);
        if (splitRemaining.length < 2) {
            throw new ResolveEndpointFailedException("Invalid Rest uri");
        }

        String method = splitRemaining[0];
        final String[] routeParts = Arrays.copyOfRange(splitRemaining, 1, splitRemaining.length);
        String route = Joiner.on(separator).join(routeParts);


        final JsonObject config = new JsonObject(parameters).copy();
        final RestChannelAdapter restChannelAdapter = new RestChannelAdapter(getVramelContext(), route, method, config);
        setProperties(restChannelAdapter, parameters);
        parameters.clear();
        return restChannelAdapter;
    }
}
