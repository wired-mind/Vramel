package com.nxttxn.vramel.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.nxttxn.vramel.ClientFactory;
import com.nxttxn.vramel.impl.jpos.JPOSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;


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
    protected final Logger logger = LoggerFactory.getLogger(DefaultClientFactory.class);
    private final Vertx vertx;
    private final Map<URI, HttpClient> httpClients = Maps.newHashMap();
    private final Map<URI, JPOSClient> jposClients = Maps.newHashMap();

    public DefaultClientFactory(Vertx vertx) {
        this.vertx = vertx;

    }

    @Override
    public HttpClient createOrFindHttpClient(URI uri, Optional<String> keystorePath, Optional<String> keystorePassword) {
        checkNotNull(uri);

        if (httpClients.containsKey(uri)) {
            return httpClients.get(uri);
        }

        return createNewHttpClient(uri, keystorePath, keystorePassword, null, null);
    }

    @Override
    public HttpClient createOrFindHttpClient(URI uri, Optional<String> keystorePath, Optional<String> keystorePassword, Optional<String> truststorePath, Optional<String> truststorePassword) {
        checkNotNull(uri);

        if (httpClients.containsKey(uri)) {
            return httpClients.get(uri);
        }

        return createNewHttpClient(uri, keystorePath, keystorePassword, truststorePath, truststorePassword);
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

    private HttpClient createNewHttpClient(URI uri, Optional<String> keystorePath, Optional<String> keystorePassword) {
        return createNewHttpClient(uri, keystorePath, keystorePassword, null, null);
    }

    private HttpClient createNewHttpClient(URI uri, Optional<String> keystorePath, Optional<String> keystorePassword, Optional<String> truststorePath, Optional<String> truststorePassword) {
        boolean ssl = false;
        if (uri.getScheme().equals("https")) {
            ssl = true;
        }
        logger.info("[DefaultClientFactory] - Creating http client : {}", uri.toString());
        HttpClient httpClient = vertx.createHttpClient().setKeepAlive(false).setMaxPoolSize(20).setHost(uri.getHost()).setSSL(ssl).setPort(uri.getPort()).setTryUseCompression(true);
        if (keystorePath.isPresent()) {
            httpClient = httpClient.setKeyStorePath(keystorePath.get()).setKeyStorePassword(keystorePassword.get());
        }

        if (truststorePath.isPresent()) {
            httpClient = httpClient.setTrustStorePath(truststorePath.get()).setTrustStorePassword(truststorePassword.get());
        }
        return httpClient;
    }


}
