package com.nxttxn.vramel;

import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 6:41 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Component extends VramelContextAware {
    Endpoint createEndpoint(String uri, JsonObject config) throws Exception;

    Endpoint createEndpoint(String uri) throws Exception;
}
