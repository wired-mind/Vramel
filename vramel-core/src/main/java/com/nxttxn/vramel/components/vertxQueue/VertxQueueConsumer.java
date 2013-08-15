package com.nxttxn.vramel.components.vertxQueue;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.components.vertx.VertxChannelAdapter;
import com.nxttxn.vramel.components.vertx.VertxConsumer;
import com.nxttxn.vramel.impl.DefaultConsumer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class VertxQueueConsumer extends DefaultConsumer {

    private final VertxQueueChannelAdapter endpoint;
    private final VertxConsumer queueDrivenConsumer;

    public VertxQueueConsumer(Endpoint endpoint, AsyncProcessor processor) throws Exception {
        super(endpoint, processor);
        this.endpoint = (VertxQueueChannelAdapter) endpoint;

        final String queueAddress = this.endpoint.getAddress();
        final String queueName = this.endpoint.getQueueName();
        final String handlerAddress = String.format("vertxQueueHandler://%s", queueAddress);
        //The queue driven consumer is the actual handler that processes the flow. The queue itself just enqueues with a hazelcast queue
        queueDrivenConsumer = new VertxConsumer(new VertxChannelAdapter(endpoint.getVramelContext(), handlerAddress, endpoint.getConfig()), processor);


        Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();

        final HazelcastInstance hzl = instances.iterator().next();

        final IQueue<String> queue = hzl.getQueue(queueName);
        logger.info(String.format("[Vertx Queue Consumer][%s] Registering vertx queue handler for: %s", queueName, queueAddress));

        getEventBus().registerHandler(queueAddress, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {


                QueueMessage msg = new QueueMessage(handlerAddress, message.body.getObject("body"), message.body.getObject("headers"));

                try {
                    logger.debug(String.format("[Vertx Queue Consumer][%s] Enqueuing message: %s", queueName, msg.toString()));
                    queue.add(msg.toJson());
                    //Mimic BusModBase behavior for now
                    message.reply(new JsonObject().putString("status", "ok"));
                } catch (Exception e) {
                    logger.error(String.format("[Vertx Queue Consumer][%s] Failed to enqueue message: %s", queueName, queueAddress), e);
                    message.reply(new JsonObject().putString("status", "error").putString("message", "Failed to enqueue message"));
                }
            }
        });
    }

    private EventBus getEventBus() {
        return endpoint.getVramelContext().getEventBus();
    }
}
