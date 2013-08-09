package com.nxttxn.vramel.impl;

import com.google.common.base.Optional;
import org.vertx.java.core.json.JsonObject;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/25/13
 * Time: 6:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class CORSSettings {

    public static final String ALLOW_METHODS = "ALLOW_METHODS";
    public static final String ENABLE_CORS = "ENABLE_CORS";
    public static final String ALLOW_HEADERS = "ALLOW_HEADERS";
    private final Optional<String> allowHeaders;
    private Optional<String> allowOrigin;
    private Optional<String> allowMethods;
    private Boolean allowCredentials;
    private final boolean enableCORS;

    public static CORSSettings create(JsonObject config) {
        return new CORSSettings(config);
    }

    protected CORSSettings(JsonObject config) {
        allowOrigin = Optional.fromNullable(config.getString("allowOrigin", null));
        allowMethods = Optional.fromNullable(config.getString(ALLOW_METHODS, null));
        allowHeaders = Optional.fromNullable(config.getString(ALLOW_HEADERS, "content-type"));
        allowCredentials = Optional.of(config.getBoolean("allowCredentials", false)).get();
        enableCORS = config.getBoolean(ENABLE_CORS, false);
    }

    public Optional<String> getAllowOrigin() {
        return allowOrigin;
    }

    public Optional<String> getAllowMethods() {
        return allowMethods;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public boolean enabled() {
        return enableCORS;
    }

    public Optional<String> getAllowHeaders() {
        return allowHeaders;
    }
}
