package com.nxttxn.vramel.model;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.processor.SendProcessor;
import com.nxttxn.vramel.spi.FlowContext;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 7:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class ToDefinition extends NoOutputDefinition<UnmarshalDefinition> {
    private final String uri;
    private final JsonObject config;

    public ToDefinition(String uri, JsonObject config) {
        this.uri = uri;
        this.config = config;
    }

    @Override
    public AsyncProcessor createProcessor(FlowContext flowContext) throws Exception {
        final Endpoint endpoint;
        if (config != null) {
            endpoint = flowContext.getVramelContext().getEndpoint(uri, config);
        } else {
            endpoint = flowContext.resolveEndpoint(uri);
        }
        return new SendProcessor(endpoint);
    }


}
