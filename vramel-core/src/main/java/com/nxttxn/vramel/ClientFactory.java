package com.nxttxn.vramel;

import com.nxttxn.vramel.impl.jpos.JPOSClient;
import org.vertx.java.core.*;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClientFactory {
    HttpClient createOrFindHttpClient(JsonObject config);

    JPOSClient createOrFindJPOSClient(URI uri, String keyFields);
}
