package com.nxttxn.vramel.components.rest;

import com.google.common.base.Optional;
import com.nxttxn.vramel.ClientFactory;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.impl.DefaultProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.impl.ws.Base64;
import org.vertx.java.core.json.JsonObject;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestProducer extends DefaultProducer {


    private final RestChannelAdapter endpoint;
    private final HttpClient httpClient;
    private final String credentials;
    //for now json is hard coded... might need to make configurable
    private final String contentType = "application/json";

    public RestProducer(Endpoint endpoint) {
        super(endpoint);
        this.endpoint = (RestChannelAdapter) endpoint;

        ClientFactory clientFactory = endpoint.getVramelContext().getClientFactory();
        final JsonObject config = endpoint.getConfig();
        httpClient = clientFactory.createOrFindHttpClient(config);
        final String username = config.getString("username");
        final String password = config.getString("password");
        credentials = encodeCredentials(username, password);
    }

    @Override
    public void process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Handler<Exception> exceptionHandler = new Handler<Exception>() {
            @Override
            public void handle(Exception e) {
                exchange.setException(e);
                optionalAsyncResultHandler.done(exchange);
            }
        };

        final String method = this.endpoint.getMethod();
        final String uri = getUri(exchange);
        HttpClientRequest request = httpClient.request(method, uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {
                logger.info(String.format("[Rest Producer] [Reply] [%s - %s]: %s - %s", method, uri, httpClientResponse.statusCode, httpClientResponse.statusMessage));
                for (Map.Entry<String, String> header : httpClientResponse.headers().entrySet()) {
                    exchange.getOut().setHeader(header.getKey(), header.getValue());
                }

                httpClientResponse.exceptionHandler(exceptionHandler);
                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {

                        logger.debug(String.format("[Rest Producer] [Reply] [%s - %s]: Response: %s", method, uri, buffer.toString()));
                        exchange.getOut().setBody(buffer.getBytes());
                        optionalAsyncResultHandler.done(exchange);
                    }
                });
            }
        });
        request.exceptionHandler(exceptionHandler);

        final byte[] body = exchange.getIn().getBody();
        final Buffer buffer = new Buffer(body == null ? new byte[0] : body);
        logger.info(String.format("[Rest Producer] [Request] [%s - %s]", method, uri));
        logger.debug(String.format("[Rest Producer] [Request] [%s - %s] - Request body: %s", method, uri, buffer.toString()));

        request = request.putHeader("Authorization", "Basic " + credentials)
                .putHeader("Accept", "*/*");

        if (buffer.length() == 0) {
            request.end();
        } else {
            request.putHeader("Content-Type", contentType)
                    .putHeader("Content-Length", buffer.length())
                    .end(buffer);
        }

    }

    private String getUri(Exchange exchange) {
        final String route = this.endpoint.getRoute();
        final String query = exchange.getIn().getHeader(Exchange.HTTP_QUERY);
        String uri = route;
        if (query != null) {
            uri = String.format("%s?%s", uri, query);
        }
        return uri;
    }

    private String encodeCredentials(String username, String password) {
        String credentials = null;
        try {
            credentials = Base64.encodeBytes(String.format("%s:%s", username, password).getBytes("UTF-8"), Base64.DONT_BREAK_LINES);
        } catch (UnsupportedEncodingException e) {
            logger.error("Cannot encode api credentials");
        }
        return credentials;
    }
}
