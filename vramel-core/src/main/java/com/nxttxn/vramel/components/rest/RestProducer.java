package com.nxttxn.vramel.components.rest;

import com.google.common.base.Optional;
import com.nxttxn.vramel.ClientFactory;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.impl.DefaultMessage;
import com.nxttxn.vramel.impl.DefaultProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 3:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestProducer extends DefaultAsyncProducer {


    private final RestChannelAdapter endpoint;
    private final HttpClient httpClient;
    private Optional<String> credentials = Optional.absent();
    //for now json is hard coded... might need to make configurable
    private final String defaultContentType = "application/json";

    public RestProducer(Endpoint endpoint) {
        super(endpoint);
        this.endpoint = (RestChannelAdapter) endpoint;

        ClientFactory clientFactory = endpoint.getVramelContext().getClientFactory();
        final String host = this.endpoint.getHost();
        final boolean ssl = this.endpoint.isSsl();
        final int port = this.endpoint.getPort();
        Optional<String> keystorePath = Optional.fromNullable(this.endpoint.getKeystorePath());
        Optional<String> keystorePassword = Optional.fromNullable(this.endpoint.getKeystorePassword());
        Optional<String> truststorePath = Optional.fromNullable(this.endpoint.getTruststorePath());
        Optional<String> truststorePassword = Optional.fromNullable(this.endpoint.getTruststorePassword());

        final String httpFormat = "http://%s:%s";
        final String httpsFormat = "https://%s:%s";
        final URI uri = URI.create(String.format(ssl ? httpsFormat : httpFormat, host, port));

        httpClient = clientFactory.createOrFindHttpClient(uri, keystorePath, keystorePassword, truststorePath, truststorePassword);
        final Optional<String> username = Optional.fromNullable(this.endpoint.getUsername());
        final String password = this.endpoint.getPassword();
        if (username.isPresent()) {
            credentials = Optional.of(encodeCredentials(username.get(), password));
        }
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Handler<Throwable> exceptionHandler = new Handler<Throwable>() {
            @Override
            public void handle(Throwable e) {
                exchange.setException(e);
                optionalAsyncResultHandler.done(exchange);
            }
        };

        final String method = this.endpoint.getMethod();
        final String uri = getUri(exchange);
        HttpClientRequest request = httpClient.request(method, uri, new Handler<HttpClientResponse>() {
            @Override
            public void handle(final HttpClientResponse httpClientResponse) {
                logger.info(String.format("[Rest Producer] [Reply] [%s - %s]: %s - %s", method, uri, httpClientResponse.statusCode(), httpClientResponse.statusMessage()));
                Message old = exchange.getIn();

                // create a new message container so we do not drag specialized message objects along
                final Message msg = new DefaultMessage();
                msg.copyFrom(old);

                for (Map.Entry<String, String> header : httpClientResponse.headers().entries()) {
                    msg.setHeader(header.getKey(), header.getValue());
                }

                httpClientResponse.exceptionHandler(exceptionHandler);
                httpClientResponse.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {

                        logger.debug(String.format("[Rest Producer] [Reply] [%s - %s]: Response: %s", method, uri, buffer.toString()));
                        msg.setBody(buffer.getBytes());
                        exchange.setOut(msg);
                        optionalAsyncResultHandler.done(exchange);
                    }
                });
            }
        });
        request.exceptionHandler(exceptionHandler);

        final Message message = exchange.getIn();
        final byte[] body = message.getMandatoryBody(byte[].class);
        final Buffer buffer = new Buffer(body == null ? new byte[0] : body);
        logger.info(String.format("[Rest Producer] [Request] [%s - %s]", method, uri));
        logger.debug(String.format("[Rest Producer] [Request] [%s - %s] - Request body: %s", method, uri, buffer.toString()));

        if (credentials.isPresent()) {
            request = request.putHeader("Authorization", "Basic " + credentials.get())
                    .putHeader("Accept", "*/*");
        }

        for (Map.Entry<String, Object> header : message.getHeaders().entrySet()) {
            request = request.putHeader(header.getKey(), (String) header.getValue());
        }

        if (buffer.length() == 0) {
            request.end();
        } else {
            // set the content type in the response.
            String contentType = message.getHeader(Exchange.CONTENT_TYPE, defaultContentType, String.class);
            message.removeHeader(Exchange.CONTENT_TYPE);

            request.putHeader("Content-Type", contentType)
                    .putHeader("Content-Length", String.valueOf(buffer.length()))
                    .end(buffer);
        }

        return false;

    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
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
