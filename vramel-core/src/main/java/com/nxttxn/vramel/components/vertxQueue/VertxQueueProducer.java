package com.nxttxn.vramel.components.vertxQueue;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.impl.DefaultAsyncProducer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 5:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxQueueProducer extends DefaultAsyncProducer {
    private final String address;

    public VertxQueueProducer(Endpoint endpoint, String address) {
        super(endpoint);
        this.address = address;
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final VramelContext vramelContext = getEndpoint().getVramelContext();


        String jsonBody = exchange.getIn().getBody(String.class);
        final QueueMessage queueMessage = QueueMessage.create(new JsonObject(jsonBody), exchange.getIn().getHeaders());

        logger.info(String.format("[Vertx Queue Producer] Sending to %s", address));
        logger.debug(String.format("[Vertx Queue Producer][Request] - %s: %s", address, queueMessage.toString()));

        vramelContext.getEventBus().send(address, queueMessage.asJson(), new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                final String status = message.body.getString("status");
                if (!status.equals("ok")) {
                    final String responseMessage = message.body.getString("message");

                    exchange.setException(new RuntimeVramelException("Failed to enqueue message using vertx queue: " + responseMessage));
                }
                optionalAsyncResultHandler.done(exchange);
            }
        });

        return false;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }
}
