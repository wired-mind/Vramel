package com.nxttxn.vramel.components.rest;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.CORSSettings;
import com.nxttxn.vramel.impl.DefaultConsumer;
import com.nxttxn.vramel.impl.HTTPListener;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestConsumer extends DefaultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(RestConsumer.class);
    public static final String FALLBACK_CONTENT_TYPE = "text/plain";
    private final RestChannelAdapter endpoint;
    private final int httpOk = 200;

    public RestConsumer(Endpoint endpoint, final Processor processor) {
        super(endpoint, processor);
        this.endpoint = (RestChannelAdapter) endpoint;

        ServerFactory serverFactory = endpoint.getVramelContext().getServerFactory();
        final JsonObject config = this.endpoint.getConfig();
        final HTTPListener listener = serverFactory.createOrFindHttpListener(config);
        final CORSSettings corsSettings = CORSSettings.create(config);

        if (corsSettings.enabled()) {
            listener.setupCORSOptionsHandler(this.endpoint.getRoute(), corsSettings);
        }

        listener.registerRoute(this.endpoint.getRoute(), this.endpoint.getMethod(), new Handler<HttpServerRequest>() {
            @Override
            public void handle(final HttpServerRequest request) {

                logger.info(String.format("[Rest Consumer] Incoming request %s - %s", request.method, request.uri));

                if (corsSettings.enabled()) {
                    listener.setCORS(request, corsSettings);
                }

                request.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer buffer) {
                        Exchange exchange = getEndpoint().createExchange();
                        final Message in = exchange.getIn();
                        in.setBody(buffer.getBytes());
                        for (Map.Entry<String, String> header : request.headers().entrySet()) {
                            in.setHeader(header.getKey(), header.getValue());
                        }

                        for (Map.Entry<String, String> param : request.params().entrySet()) {
                            in.setHeader(param.getKey(), param.getValue());
                        }

                        final String defaultContentType = in.getHeader(Exchange.CONTENT_TYPE, FALLBACK_CONTENT_TYPE, String.class);
                        in.setHeader(Exchange.HTTP_METHOD, request.method);

                        try {
                            logger.debug("[Rest Consumer] Ready to process exchange: {}.", processor.toString(), exchange);
                            getAsyncProcessor().process(exchange, createResponseHandler(request.response, defaultContentType));
                        } catch (Exception e) {
                            handleInternalError(exchange, e, request.response, defaultContentType);
                        }
                    }
                });
            }
        });
    }

    private OptionalAsyncResultHandler createResponseHandler(final HttpServerResponse response, final String defaultContentType) {
        return new OptionalAsyncResultHandler() {
            @Override
            public void handle(AsyncExchangeResult optionalAsyncResult) {
                final Exchange exchange = optionalAsyncResult.result.get();
                if (optionalAsyncResult.failed()) {
                    handleInternalError(exchange, optionalAsyncResult.getException(), response, defaultContentType);
                    return;
                }
                final Message out = exchange.getOut();

                doWriteResponse(out, response, defaultContentType);
            }
        };
    }

    private void doWriteResponse(Message message, HttpServerResponse response, String defaultContentType) {
        int statusCode = httpOk;
        final boolean noResponseBody = message.getBody() == null || message.getBody() == Void.TYPE;
        final byte[] body = message.getBody(byte[].class);


        final boolean validResponseBody = body != null || noResponseBody;

        if (!validResponseBody) {
            throw new RuntimeVramelException("[Rest Consumer] The message contains a body but it is not a String or byte[]. Currently Rest Consumer is only smart enough to handle these types. If your intention is to provide a response, please set the message body using one of these types.");
        }

        if (message.getHeader(Exchange.HTTP_RESPONSE_CODE) != null) {
            statusCode = message.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        } else {
            if (noResponseBody) {
                statusCode = 204;
            }
        }
        // set the content type in the response.
        String contentType = message.getHeader(Exchange.CONTENT_TYPE, defaultContentType, String.class);
        if (contentType != null) {
            response.putHeader("Content-Type", contentType);
        }



        response.setChunked(true); // or set Content-length
        String statusMessage = "OK";
        if (message.getHeader(Exchange.HTTP_STATUS_MESSAGE) != null) {
            statusMessage = message.getHeader(Exchange.HTTP_STATUS_MESSAGE, String.class);
        }
        response.statusMessage = statusMessage;    //should be tied to statuscode probably


        Buffer buffer;
        if (body != null) {
            buffer = new Buffer(body);
        } else {
            buffer = new Buffer();
        }


        response.statusCode = statusCode;
        logger.info(String.format("Http result handler ready to respond with status: %s", response.statusCode));
        response.end(buffer);
    }

    private void handleInternalError(Exchange exchange, Throwable ex, HttpServerResponse response, String defaultContentType) {
        response.setChunked(true); // or set Content-length
        response.putHeader("Content-Type", defaultContentType);
        response.statusCode = 500;
        response.statusMessage = "Internal Server Error";
        response.write(new JsonObject().putString("error", "Internal server error").toString());
        response.end();
        logError(ex);
    }

}
