package com.nxttxn.vramel.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nxttxn.vramel.ServerFactory;
import com.nxttxn.vramel.impl.jpos.JPOSServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 12:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultServerFactory implements ServerFactory {
    private final Logger logger = LoggerFactory.getLogger(DefaultServerFactory.class);
    private final Vertx vertx;
    private final List<HTTPListener> httpListeners = Lists.newArrayList();
    private final Map<URI, JPOSServer> jposServers = Maps.newHashMap();


    public DefaultServerFactory(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public HTTPListener createOrFindHttpListener(JsonObject config) {
        final HTTPListener newHttpListener = createNewHttpListener(config);
        if (httpListeners.contains(newHttpListener)) {
            final int index = httpListeners.indexOf(newHttpListener);
            return httpListeners.get(index);
        }
        logger.info("Created new http listener: {} {}", newHttpListener.getHost(), newHttpListener.getPort());
        httpListeners.add(newHttpListener);
        return newHttpListener;
    }

    @Override
    public JPOSServer createOrFindJPOSServer(URI uri) {

        if (jposServers.containsKey(uri)) {
            return jposServers.get(uri);
        }

        final JPOSServer jposServer = createNewJPOSServer();
        jposServers.put(uri, jposServer);
        return jposServer;
    }

    private JPOSServer createNewJPOSServer() {
        return new JPOSServer(vertx);
    }

    @Override
    public void startAllServers() {
        for (HTTPListener httpListener : httpListeners) {

            logger.info("Starting http server: {} {}", httpListener.getHost(), httpListener.getPort());
            httpListener.start();
        }

        for (Map.Entry<URI, JPOSServer> serverEntry : jposServers.entrySet()) {
            final URI uri = serverEntry.getKey();
            final JPOSServer jposServer = serverEntry.getValue();
            jposServer.listen(uri.getPort(), uri.getHost());
        }
    }

    private HTTPListener createNewHttpListener(JsonObject config) {
        HTTPListener listener = new HTTPListener(vertx, config);
        return listener;
    }
}
