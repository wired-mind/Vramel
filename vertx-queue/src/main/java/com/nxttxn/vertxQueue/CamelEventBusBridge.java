package com.nxttxn.vertxQueue;

import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.components.vertx.VertxMessage;
import com.nxttxn.vramel.components.vertxQueue.QueueMessage;
import com.nxttxn.vramel.impl.DefaultExchange;
import com.nxttxn.vramel.impl.DefaultExchangeHolder;
import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public class CamelEventBusBridge implements AsyncProcessor {

    private final long timeout = 30L;

    private VertxContext vertxContext;

    private static final Logger LOG = LoggerFactory.getLogger(CamelEventBusBridge.class);

    public CamelEventBusBridge() {


    }



    public void process(Exchange exchange) {

    }

    public boolean process(final Exchange exchange, final AsyncCallback callback)  {
        Vertx vertx = getVertxContext().getVertx();

        final CountDownLatch vertxLatch = new CountDownLatch (1);
        try {

            QueueMessage msg = new QueueMessage((String) exchange.getIn().getBody());
            VertxMessage vertxMessage = VertxMessage.create(msg);
            LOG.info(String.format("Vertx Queue Bridge ready to send message to vertx: %s", msg.toString()));

            vertx.eventBus().send(msg.getHandlerUri(), SerializationUtils.serialize(vertxMessage), new Handler<Message<byte[]>>() {
                @Override
                public void handle(Message<byte[]> message) {

                    com.nxttxn.vramel.Exchange responseExchange = new DefaultExchange((VramelContext) null);
                    try {
                        DefaultExchangeHolder.unmarshal(responseExchange, message.body);
                        if (responseExchange.isFailed()) {
                            exchange.setException(new EventBusException("Vertx Queue Processor: Attempted to send message to vertx and failed.", responseExchange.getException()));
                        }
                    } catch (Exception e) {
                        exchange.setException(new EventBusException("Vertx Queue Processor: Attempted to send message to vertx and failed. Could not parse response. Expected Vramel Exchange.", e));
                    }

                    callback.done(true); //async isn't setup anyway for hazelcast!!!
                    vertxLatch.countDown();
                }
            });


            if (!vertxLatch.await (timeout, TimeUnit.SECONDS)) {
                exchange.setException(new EventBusException("Vertx exchange timed out."));
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
    }


}