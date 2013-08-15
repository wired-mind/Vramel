package com.nxttxn.vramel.components.vertx;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.impl.DefaultExchangeHolder;
import com.nxttxn.vramel.impl.DefaultProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxProducer extends DefaultAsyncProducer {

    private final String address;

    public VertxProducer(Endpoint endpoint, String address) throws Exception {
        super(endpoint);
        this.address = address;
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final VramelContext vramelContext = getEndpoint().getVramelContext();

        byte[] obj;

        final boolean isTransferExchange = getEndpoint().getConfig().getBoolean("isTransferExchange", true);
        if (isTransferExchange) {
            final DefaultExchangeHolder holder = DefaultExchangeHolder.marshal(exchange);
            obj = holder.getBytes();
        } else {
            obj = exchange.getIn().getBody(byte[].class);
        }

        logger.info(String.format("[Vertx Producer] Sending to %s", address));
        logger.debug(String.format("[Vertx Producer][Request] - %s: %s", address, new String(obj)));


        if (!byte[].class.isInstance(obj)) {
            throw new RuntimeVramelException("Message body must be serialized before sending on vertx event bus");
        }
        vramelContext.getEventBus().send(address, obj, new Handler<Message<byte[]>>() {
            @Override
            public void handle(Message<byte[]> message) {
                if (!isTransferExchange) {
                    exchange.getOut().setBody(message.body);
                    optionalAsyncResultHandler.done(exchange);
                } else {
                    Exchange responseExchange = exchange;
                    try {
                        DefaultExchangeHolder.unmarshal(responseExchange, message.body);
                    } catch (Exception e) {
                        logger.error("Error unmarshaling vertx response", e);
                        exchange.setException(new RuntimeVramelException("Cannot read the vertx response.", e));
                        optionalAsyncResultHandler.done(exchange);
                        return;
                    }

                    logger.debug(String.format("[Vertx Producer][Response] - %s: %s", address, responseExchange));
                    optionalAsyncResultHandler.done(responseExchange);
                }
            }
        });
        return false;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }
}
