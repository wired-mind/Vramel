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


    /**
     * Whether to use raw or encoded uri, when creating endpoints.
     * <p/>
     * <b>Notice:</b> When using raw uris, then the parameter values is raw as well.
     *
     * @return <tt>true</tt> to use raw uris, <tt>false</tt> to use encoded uris (default).
     *
     * @since Camel 2.11.0
     */
    boolean useRawUri();
}
