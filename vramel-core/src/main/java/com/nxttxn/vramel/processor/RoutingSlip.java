package com.nxttxn.vramel.processor;

import com.google.common.base.Optional;
import com.nxttxn.vramel.*;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.impl.DefaultExchange;
import com.nxttxn.vramel.impl.ProducerCache;
import com.nxttxn.vramel.processor.async.DefaultExchangeHandler;
import com.nxttxn.vramel.processor.async.DoneStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.PipelineSupport;
import com.nxttxn.vramel.util.*;
import org.vertx.java.core.AsyncResultHandler;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.nxttxn.vramel.util.ExchangeHelper.copyResults;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class RoutingSlip extends PipelineSupport implements AsyncProcessor {
    protected final VramelContext vramelContext;
    protected String uriDelimiter;
    protected Expression expression;
    private ProducerCache producerCache;

    /**
     * The iterator to be used for retrieving the next routing slip(s) to be used.
     */
    protected interface RoutingSlipIterator {

        /**
         * Are the more routing slip(s)?
         *
         * @param exchange the current exchange
         * @return <tt>true</tt> if more slips, <tt>false</tt> otherwise.
         */
        boolean hasNext(Exchange exchange);

        /**
         * Returns the next routing slip(s).
         *
         * @param exchange the current exchange
         * @return the slip(s).
         */
        Object next(Exchange exchange);

    }

    public RoutingSlip(VramelContext vramelContext) {
        checkNotNull(vramelContext);
        this.vramelContext = vramelContext;
    }

    public RoutingSlip(VramelContext vramelContext, Expression expression, String uriDelimiter) {
        checkNotNull(vramelContext);
        checkNotNull(expression);
        this.vramelContext = vramelContext;
        this.expression = expression;
        this.uriDelimiter = uriDelimiter;
    }


    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        doRoutingSlip(exchange, optionalAsyncResultHandler);
        return false;
    }

    public void doRoutingSlip(Exchange exchange, Object routingSlip, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        if (routingSlip instanceof Expression) {
            this.expression = (Expression) routingSlip;
        } else {
            this.expression = ExpressionBuilder.constantExpression(routingSlip);
        }
        doRoutingSlip(exchange, optionalAsyncResultHandler);
    }

    private void doRoutingSlip(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        Exchange current = exchange;
        RoutingSlipIterator iter;
        try {
            iter = createRoutingSlipIterator(exchange);
        } catch (Exception e) {
            exchange.setException(e);
            optionalAsyncResultHandler.done(exchange);
            return;
        }

        // ensure the slip is empty when we start
        if (current.hasProperties()) {
            current.setProperty(Exchange.SLIP_ENDPOINT, null);
        }

        process(exchange, current, optionalAsyncResultHandler, iter);
    }


    private void process(final Exchange original, final Exchange current, final OptionalAsyncResultHandler optionalAsyncResultHandler, final RoutingSlipIterator routingSlips) throws Exception {

        if (!routingSlips.hasNext(current)) {
            copyResults(original, current);
            optionalAsyncResultHandler.done(original);
            return;
        }

        Endpoint endpoint;
        try {
            endpoint = resolveEndpoint(routingSlips, current);
            // if no endpoint was resolved then try the next
            if (endpoint == null) {
                optionalAsyncResultHandler.done(current);
                return;
            }
        } catch (Exception e) {
            // error resolving endpoint so we should break out
            current.setException(e);
            optionalAsyncResultHandler.done(current);
            return;
        }

        final Producer producer = producerCache.acquireProducer(endpoint);

        // set property which endpoint we send to
        current.setProperty(Exchange.TO_ENDPOINT, endpoint.getEndpointUri());
        current.setProperty(Exchange.SLIP_ENDPOINT, endpoint.getEndpointUri());
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(producer);
        ap.process(current, new RoutingSlipResultsHandler(optionalAsyncResultHandler, routingSlips, current, original));


    }

    /**
     * Creates the route slip iterator to be used.
     *
     * @param exchange the exchange
     * @return the iterator, should never be <tt>null</tt>
     */
    protected RoutingSlipIterator createRoutingSlipIterator(final Exchange exchange) throws Exception {
        Object slip = expression.evaluate(exchange, Object.class);
        if (exchange.getException() != null) {
            // force any exceptions occurred during evaluation to be thrown
            throw exchange.getException();
        }

        final Iterator<Object> delegate = ObjectHelper.createIterator(slip, uriDelimiter);

        return new RoutingSlipIterator() {
            public boolean hasNext(Exchange exchange) {
                return delegate.hasNext();
            }

            public Object next(Exchange exchange) {
                return delegate.next();
            }
        };
    }


    protected Endpoint resolveEndpoint(RoutingSlipIterator iter, Exchange exchange) throws Exception {
        Object nextRecipient = iter.next(exchange);
        return vramelContext.getEndpoint((String) nextRecipient);
    }


    protected Exchange prepareExchangeForRoutingSlip(Exchange current, Endpoint endpoint) {
        Exchange copy = new DefaultExchange(current);
        // we must use the same id as this is a snapshot strategy where Camel copies a snapshot
        // before processing the next step in the pipeline, so we have a snapshot of the exchange
        // just before. This snapshot is used if Camel should do redeliveries (re try) using
        // DeadLetterChannel. That is why it's important the id is the same, as it is the *same*
        // exchange being routed.
//        copy.setExchangeId(current.getExchangeId());
        copyOutToIn(copy, current);
        return copy;
    }

    /**
     * Copy the outbound data in 'source' to the inbound data in 'result'.
     */
    private void copyOutToIn(Exchange result, Exchange source) {
        result.setException(source.getException());

        if (source.hasOut() && source.getOut().isFault()) {
            result.getOut().copyFrom(source.getOut());
        }

        result.setIn(getResultMessage(source));

        result.getProperties().clear();
        result.getProperties().putAll(source.getProperties());
    }

    /**
     * Returns the outbound message if available. Otherwise return the inbound message.
     */
    private Message getResultMessage(Exchange exchange) {
        if (exchange.hasOut()) {
            return exchange.getOut();
        } else {
            // if this endpoint had no out (like a mock endpoint) just take the in
            return exchange.getIn();
        }
    }

    private class RoutingSlipResultsHandler extends DefaultExchangeHandler {
        private final RoutingSlipIterator routingSlips;
        private final Exchange current;
        private final Exchange original;

        public RoutingSlipResultsHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, RoutingSlipIterator routingSlips, Exchange current, Exchange original) {
            super(original, optionalAsyncResultHandler, new RoutingSlipDoneStrategy(routingSlips));
            this.routingSlips = routingSlips;
            this.current = current;
            this.original = original;
        }





        @Override
        protected void proceed(Optional<Exchange> currentResult) throws Exception {
            Exchange nextExchange = createNextExchange(currentResult.or(current));
            logger.info(String.format("Ready to process next processor using exchange: %s", nextExchange.toString()));
            process(original, nextExchange, optionalAsyncResultHandler, routingSlips);
        }

        @Override
        protected String getName() {
            return "Routing slip results handler";
        }


    }

    private class RoutingSlipDoneStrategy implements DoneStrategy<Exchange> {
        private final RoutingSlipIterator routingSlips;

        public RoutingSlipDoneStrategy(RoutingSlipIterator routingSlips) {
            this.routingSlips = routingSlips;
        }

        @Override
        public boolean isDone(Exchange exchange) {
            //The routing slip is never done. Routing is finished when the iterator says so.
            return false;
        }
    }

    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        if (producerCache == null) {
            producerCache = new ProducerCache(this, vramelContext);
        }
        ServiceHelper.startService(producerCache);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopService(producerCache);
    }

    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownService(producerCache);
    }

}
