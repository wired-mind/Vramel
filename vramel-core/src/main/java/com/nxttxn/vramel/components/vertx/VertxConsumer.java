package com.nxttxn.vramel.components.vertx;

import com.google.common.base.Optional;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultConsumer;
import com.nxttxn.vramel.impl.DefaultExchange;
import com.nxttxn.vramel.impl.DefaultExchangeHolder;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.apache.commons.lang3.SerializationUtils;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class VertxConsumer extends DefaultConsumer {

    private final VertxChannelAdapter endpoint;

    public VertxConsumer(final Endpoint endpoint, final Processor processor) throws Exception {
        super(endpoint, processor);
        this.endpoint = (VertxChannelAdapter) endpoint;

        getEventBus().registerHandler(this.endpoint.getAddress(), new Handler<Message<byte[]>>() {

            @Override
            public void handle(final Message<byte[]> message) {

                logger.info("[Vertx Consumer] [{}] Received Message", ((VertxChannelAdapter) endpoint).getAddress());
                Exchange exchange = getEndpoint().createExchange();
                try {
                    DefaultExchangeHolder.unmarshal(exchange, message.body);
                } catch (Exception e) {
                    try {
                        final VertxMessage vertxMessage = (VertxMessage) SerializationUtils.deserialize(message.body);
                        exchange.getIn().setBody(vertxMessage.getBody());
                        exchange.getIn().setHeaders(vertxMessage.getHeaders());
                    } catch (Exception e1) {
                        exchange.getIn().setBody(message.body);
                    }
                }

                try {
                    final Exchange request = exchange;

                    logger.debug("[Vertx Consumer] received message: " + exchange.toString());
                    processor.process(request, new OptionalAsyncResultHandler() {
                        @Override
                        public void handle(AsyncExchangeResult optionalAsyncResult) {
                            if (optionalAsyncResult.failed()) {
                                request.setException(optionalAsyncResult.getException());
                                sendError(message, request);
                                return;
                            }

                            final Optional<Exchange> result = optionalAsyncResult.result;
                            if (result.isPresent()) {
                                replyWithExchange(message, result.get());
                            } else {
                                replyWithExchange(message, request);
                            }
                        }
                    });
                } catch (Exception e) {
                    logger.error(String.format("[Vertx Consumer] Error processing flow: %s", ((VertxChannelAdapter) endpoint).getAddress()), e);
                    exchange.setException(new RuntimeVramelException("Vertx consumer failed to process message: " + e.getMessage()));
                    sendError(message, exchange);
                }
            }


        });
    }

    private EventBus getEventBus() {
        return endpoint.getVramelContext().getEventBus();
    }


    protected void sendError(Message<byte[]> message, Exchange exchange) {
        logger.error("Error during the exchange processing", exchange.getException());
        replyWithExchange(message, exchange);
    }

    private void replyWithExchange(Message<byte[]> message, Exchange exchange) {

        try {
            DefaultExchangeHolder holder = DefaultExchangeHolder.marshal(exchange);

            message.reply(holder.getBytes());
        } catch (Exception e) {
            logger.error("Unable to marshal the exchange for return", e);
        }
    }
}
