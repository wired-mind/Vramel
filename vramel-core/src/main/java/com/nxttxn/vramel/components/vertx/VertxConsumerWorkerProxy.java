package com.nxttxn.vramel.components.vertx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.deploy.Verticle;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 10/3/13
 * Time: 5:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxConsumerWorkerProxy extends Verticle {
    protected final Logger logger = LoggerFactory.getLogger(VertxConsumerWorkerProxy.class);
    @Override
    public void start() throws Exception {
        final JsonObject config = getContainer().getConfig();
        final String proxyAddress = config.getString("proxyAddress");
        final String address = config.getString("address");

        final EventBus eventBus = getVertx().eventBus();
        eventBus.registerHandler(proxyAddress, new Handler<Message<byte[]>>() {
            @Override
            public void handle(final Message<byte[]> message) {
                logger.debug(String.format("Proxying message from %s to %s.", proxyAddress, address));
                eventBus.send(address, message.body, new Handler<Message<byte[]>>() {
                    @Override
                    public void handle(Message<byte[]> response) {
                        message.reply(response.body);
                    }
                });

            }
        });
    }
}
