package com.nxttxn.vramel.processor;

import com.google.common.base.Optional;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.async.*;
import com.nxttxn.vramel.support.AggregationSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class Enricher extends AggregationSupport implements AsyncProcessor {
    private final Producer producer;

    public Enricher(AggregationStrategy aggregationStrategy, Producer producer) {
        super(aggregationStrategy);

        this.producer = producer;
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(producer);
        final boolean sync = ap.process(exchange.copy(), new EnricherHandler(optionalAsyncResultHandler, exchange, aggregationStrategy));

        return sync;
    }

    private class EnricherHandler extends AggregatingExchangeHandlerBase {


        public EnricherHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, Exchange original, AggregationStrategy aggregationStrategy) {
            super(original, optionalAsyncResultHandler, new FixedSizeDoneStrategy(1, new AtomicInteger(0)), aggregationStrategy);
        }


        @Override
        protected Optional<Exchange> processResult(Optional<Exchange> currentResult) {
            final Exchange replyExchange = currentResult.get();

            final Exchange aggregateExchange = doAggregate(original, replyExchange);
            return Optional.of(aggregateExchange);
        }

        @Override
        protected Optional<Exchange> getFinalResult(Optional<Exchange> finalResult) {
            final Exchange aggregatedExchange = finalResult.get();
            doAggregationComplete(aggregatedExchange);
            copyResults(original, aggregatedExchange);
            return Optional.of(original);
        }


        @Override
        protected String getName() {
            return "Enricher aggregating handler";
        }
    }
}
