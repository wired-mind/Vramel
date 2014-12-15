package com.nxttxn.vertxQueue;

import com.nxttxn.vramel.ProducerTemplate;
import com.nxttxn.vramel.components.vertx.VertxMessage;
import com.nxttxn.vramel.components.vertxQueue.QueueMessage;
import com.nxttxn.vramel.impl.DefaultVramelContext;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CamelEventBusBridge implements AsyncProcessor {

    private VertxContext vertxContext;

    private static final Logger LOG = LoggerFactory.getLogger(CamelEventBusBridge.class);
    private DefaultVramelContext vramelContext;

    public CamelEventBusBridge() {

    }

    public void process(Exchange exchange) {

    }

    public boolean process(final Exchange exchange, final AsyncCallback callback)  {

        final CountDownLatch vertxLatch = new CountDownLatch (1);
        try {

            QueueMessage msg = new QueueMessage((String) exchange.getIn().getBody());
            VertxMessage vertxMessage = VertxMessage.create(msg);
            LOG.info(String.format("Vertx Queue Bridge ready to send message to vertx: %s", msg.toString()));

            final ProducerTemplate producerTemplate = vramelContext.createProducerTemplate();

            final String endpointUri = String.format("vertx:%s", msg.getHandlerUri());
            producerTemplate.requestBodyAndHeaders(endpointUri, vertxMessage.getBody(), vertxMessage.getHeaders(), new AsyncResultHandler<Object>() {
                @Override
                public void handle(AsyncResult<Object> event) {
                    if (event.failed()) {
                        LOG.error("Failed to send vertxQueue message to: "+ endpointUri, event.cause());
                        exchange.setException(new EventBusException("Vertx Queue Processor: Attempted to send message to vertx and failed.", new Exception(event.cause())));
                    } else {
                        LOG.info("Successfully delivered vertxQueue message to: " + endpointUri);
                    }
                    callback.done(true); //async isn't setup anyway for hazelcast!!!
                    vertxLatch.countDown();
                }
            });


            if (!vertxLatch.await (CamelConsoleMain.getEventBusSendTimeout(), TimeUnit.SECONDS)) {
                LOG.error("Timed out waiting for response from vertxQueue endpoint: "+ endpointUri);
                exchange.setException(new EventBusException("Vertx exchange timed out."));
            } else {
                LOG.info("Sent vertxQueue message to: " + endpointUri);
            }

        }
        catch (Exception e) {
            LOG.error("Error processing event bus bridge queue", e);
        }

        return true;

    }

    public VertxContext getVertxContext() {
        return vertxContext;
    }

    public void setVertxContext(VertxContext vertxContext) {
        this.vertxContext = vertxContext;
        this.vramelContext = new DefaultVramelContext(vertxContext.getVertx());
    }


}