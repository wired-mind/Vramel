package com.nxttxn.vramel.impl;

import com.google.common.collect.Maps;
import com.nxttxn.vramel.ClientFactory;
import com.nxttxn.vramel.impl.jpos.JPOSClient;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;


import java.net.URI;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultClientFactory implements ClientFactory {
    private final Vertx vertx;
    private final Map<URI, HttpClient> httpClients = Maps.newHashMap();
    private final Map<URI, JPOSClient> jposClients = Maps.newHashMap();

    public DefaultClientFactory(Vertx vertx) {
        this.vertx = vertx;

    }

    @Override
    public HttpClient createOrFindHttpClient(JsonObject config) {
        checkNotNull(config);
        final String host = config.getString("host");
        final boolean ssl = config.getBoolean("ssl", true);
        final Number port = config.getNumber("port", 443);
        final String httpFormat = "http://%s:%s";
        final String httpsFormat = "https://%s:%s";
        final URI uri = URI.create(String.format(ssl ? httpsFormat : httpFormat, host, port));


        if (httpClients.containsKey(uri)) {
            return httpClients.get(uri);
        }

        return createNewHttpClient(uri);
    }

    @Override
    public JPOSClient createOrFindJPOSClient(URI uri, String keyFields) {
        checkNotNull(uri);
        if (jposClients.containsKey(uri)) {
            return jposClients.get(uri);
        }

        final JPOSClient newJPOSClient = createNewJPOSClient(uri, keyFields);
        jposClients.put(uri, newJPOSClient);
        return newJPOSClient;
    }

    private JPOSClient createNewJPOSClient(URI uri, String keyFields) {
        return new JPOSClient(vertx, uri, keyFields);
    }

    private HttpClient createNewHttpClient(URI uri) {
        boolean ssl = false;
        if (uri.getScheme().equals("https")) {
            ssl = true;
        }

        return vertx.createHttpClient().setKeepAlive(false).setMaxPoolSize(20).setHost(uri.getHost()).setSSL(ssl).setPort(uri.getPort());
    }


}
