package com.nxttxn.vramel;

import com.nxttxn.vramel.impl.HTTPListener;
import com.nxttxn.vramel.impl.jpos.JPOSServer;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ServerFactory {
    HTTPListener createOrFindHttpListener(JsonObject config);

    void startAllServers();

    JPOSServer createOrFindJPOSServer(URI uri);
}
