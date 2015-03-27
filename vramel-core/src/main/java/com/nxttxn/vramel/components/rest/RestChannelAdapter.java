package com.nxttxn.vramel.components.rest;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultEndpoint;
import com.nxttxn.vramel.spi.HeaderFilterStrategy;
import com.nxttxn.vramel.spi.HeaderFilterStrategyAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestChannelAdapter extends DefaultEndpoint implements HeaderFilterStrategyAware {
    private static final Logger logger = LoggerFactory.getLogger(RestChannelAdapter.class);
    private HeaderFilterStrategy headerFilterStrategy = new HttpHeaderFilterStrategy();
    private final String route;
    private final String method;
    private final JsonObject config;

    private String host = "localhost";
    private int port = 8080;
    private boolean ssl = false;
    private String keystorePath;
    private String keystorePassword;
    private String truststorePath;
    private String truststorePassword;

    //basic auth
    private String username;
    private String password;

    //cors (just a subset for now)
    private boolean allowCredentials;

    public RestChannelAdapter(VramelContext vramelContext, String route, String method, JsonObject config) {
        super(String.format("rest:%s:%s", method, route), vramelContext);

        this.route = route;
        this.method = method;
        this.config = config;
    }

    @Override
    public Consumer createConsumer(final Processor processor) {
        return new RestConsumer(this, processor);
    }

    @Override
    public Producer createProducer() {
        return new RestProducer(this);
    }


    public String getMethod() {
        return method;
    }

    public String getRoute() {
        return route;
    }

    public JsonObject getConfig() {
        return config;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getTruststorePath() {
        return truststorePath;
    }

    public void setTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
    }

    public String getTruststorePassword() {
        return truststorePassword;
    }

    public void setTruststorePassword(String truststorePassword) {
        this.truststorePassword = truststorePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    public void setHeaderFilterStrategy(HeaderFilterStrategy headerFilterStrategy) {
        this.headerFilterStrategy = headerFilterStrategy;
    }
}
