package com.nxttxn.vramel.impl;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.*;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/17/13
 * Time: 3:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class HTTPListener {

    private static final Logger logger = LoggerFactory.getLogger(HTTPListener.class);

    private final RouteMatcher routeMatcher = new RouteMatcher();
    private final Vertx vertx;
    private final Number port;
    private final String host;
    private final Optional<String> keystorePath;
    private final Optional<String> keystorePassword;
    private HttpServer httpServer;



    public HTTPListener(Vertx vertx, final JsonObject config) {

        this.vertx = vertx;
        port = config.getNumber("port", 80);
        host = config.getString("host", "localhost");
        keystorePath = Optional.fromNullable(config.getString("keystorePath", null));
        keystorePassword = Optional.fromNullable(config.getString("keystorePassword", null));


        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                logger.info("Incoming request: {} {} {}", request.method(), request.path(), request.query());
                request.response().setStatusCode(405);
                request.response().setStatusMessage(request.method().toUpperCase() + " Request Not Allowed");
                request.response().end();
            }
        });
        routeMatcher.get("/status", new Handler<HttpServerRequest>() {
            @Override
            public void handle(HttpServerRequest request) {
                setCORS(request, CORSSettings.create(config));
                request.response().end("Ok");
            }
        });


    }

    public void registerRoute(String route, String method, Handler<HttpServerRequest> handler) {
        logger.info("Registering route {} {}", route, method);
        switch (method) {
            case "GET":
                routeMatcher.get(route, handler);
                break;
            case "POST":
                routeMatcher.post(route, handler);
                break;
            case "PUT":
                routeMatcher.put(route, handler);
                break;
            case "DELETE":
                routeMatcher.delete(route, handler);
                break;
            default:
                logger.warn("No routing setup yet for this method, %s", method);
                break;
        }
    }

    public void start() {
        httpServer = vertx.createHttpServer()
                .requestHandler(routeMatcher);
        if (keystorePath.isPresent()) {
            httpServer = httpServer.setSSL(true)
                    .setKeyStorePath(keystorePath.get())
                    .setKeyStorePassword(keystorePassword.get());
        }
        httpServer = httpServer.listen(getPort().intValue(), getHost());

        logger.info(String.format("HttpServer listening on port %s", getPort()));
    }


    public Number getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HTTPListener listener = (HTTPListener) o;

        if (!host.equals(listener.host)) return false;
        if (!port.equals(listener.port)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = port.hashCode();
        result = 31 * result + host.hashCode();
        return result;
    }

    public Handler<HttpServerRequest> createCORSOptionsHandler(final CORSSettings corsSettings) {
        return new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                if (logger.isTraceEnabled()) logger.trace("In CORS options handler");
                req.response().headers().set("Cache-Control", "public,max-age=31536000");
                long oneYearSeconds = 365 * 24 * 60 * 60;
                long oneYearms = oneYearSeconds * 1000;
                String expires = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz").format(new Date(System.currentTimeMillis() + oneYearms));
                req.response().headers().set("Expires", expires);
                req.response().headers().set("Access-Control-Allow-Methods", corsSettings.getAllowMethods().get());
                req.response().headers().set("Access-Control-Max-Age", String.valueOf(oneYearSeconds));
                req.response().headers().set("Access-Control-Allow-Headers", corsSettings.getAllowHeaders().get());
                setCORS(req, corsSettings);
                req.response().setStatusCode(204);
                req.response().end();
            }
        };
    }

    public void setCORS(HttpServerRequest req, CORSSettings corsSettings) {

        if (logger.isTraceEnabled()) logger.trace("Setting CORS");
        final Optional<String> allowOrigin = corsSettings.getAllowOrigin();
        if (allowOrigin.isPresent()) {
            req.response().headers().set("Access-Control-Allow-Origin", allowOrigin.get());
            req.response().headers().set("Access-Control-Allow-Credentials", corsSettings.getAllowCredentials().toString());
            req.response().headers().set("Access-Control-Expose-Headers", corsSettings.getAllowHeaders().get());

        }
    }


    public void setupCORSOptionsHandler(String route, CORSSettings corsSettings) {
        Handler<HttpServerRequest> corsOptionsHandler = createCORSOptionsHandler(corsSettings);
        logger.info("Registering route {} {}", route, "OPTIONS");

        routeMatcher.options(route, corsOptionsHandler);
    }
}
