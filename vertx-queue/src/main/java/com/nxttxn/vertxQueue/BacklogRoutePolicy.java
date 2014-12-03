package com.nxttxn.vertxQueue;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import org.apache.camel.Exchange;
import org.apache.camel.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/21/13
 * Time: 2:04 PM
 * Support Send-backlog-flush pattern. After a failed exchange, we flag the failed message destination and filter it from the "flush" route. This way
 * failed exchanges will start to queue up in the backlog. After a successful exchange, we unflag that destination which will
 * flush messages from the backlog, back into the primary workqueue.
 */
public class BacklogRoutePolicy extends org.apache.camel.impl.RoutePolicySupport {
    private static final Logger LOG = LoggerFactory.getLogger(BacklogRoutePolicy.class);

    private final IList<String> backloggedHandlers;


    public BacklogRoutePolicy() {
        super();

        Set<HazelcastInstance> instances = Hazelcast.getAllHazelcastInstances();

        HazelcastInstance hzl = instances.iterator().next();
        backloggedHandlers = hzl.getList(String.format("%s.backloggedHandlers", BacklogRoutePolicy.class.getCanonicalName()));
    }

    @Override
    public void onExchangeDone(Route route, Exchange exchange) {

        Boolean sentToBacklog = wasSentToBacklog(exchange);
        Boolean sentToEventBus = wasSentToEventBus(exchange);


        BusMessage msg = new BusMessage((String) exchange.getIn().getBody());

        if (sentToBacklog) {
            if (backloggedHandlers.add(msg.getHandlerUri())) {
                LOG.info(String.format("%s handler is now backlogged.", msg.getHandlerUri()));
            }
        } else if (sentToEventBus) {
            if (backloggedHandlers.remove(msg.getHandlerUri())) {
                LOG.info(String.format("%s handler is removed from backlog. Any backlogged messages will now be flushed.", msg.getHandlerUri()));
            }
        }
    }

    private Boolean wasSentToEventBus(Exchange exchange) {
        final Object sentToEventBusObj = exchange.getProperty("sentToEventBus");
        Boolean sentToEventBus = false;
        if (sentToEventBusObj != null) {
            sentToEventBus = Boolean.parseBoolean(sentToEventBusObj.toString());
        }
        return sentToEventBus;
    }

    private Boolean wasSentToBacklog(Exchange exchange) {
        final Object sentToBacklogObj = exchange.getProperty("sentToBacklog");
        Boolean sentToBacklog = false;
        if (sentToBacklogObj != null) {
            sentToBacklog = Boolean.parseBoolean(sentToBacklogObj.toString());
        }
        return sentToBacklog;
    }


    //We maintain a list in hazelcast of which handlerURI's have failed.
    public boolean lastExchangeSuccessful(Exchange exchange) {
        BusMessage msg = new BusMessage((String) exchange.getIn().getBody());
        final boolean lastExchangeFailed = backloggedHandlers.contains(msg.getHandlerUri());
        if(!lastExchangeFailed) {
            LOG.info("Backlog for "+msg.getHandlerUri()+" has cleared. Flushing backlog queue");
        } else {
            LOG.warn("vertxQueue for "+msg.getHandlerUri()+" is still backlogged. Cannot clear yet.");
        }
        return !lastExchangeFailed;
    }
}
