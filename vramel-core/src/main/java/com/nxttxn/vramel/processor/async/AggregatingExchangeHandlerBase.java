package com.nxttxn.vramel.processor.async;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.CompletionAwareAggregationStrategy;
import com.nxttxn.vramel.util.ExchangeHelper;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/28/13
 * Time: 12:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class AggregatingExchangeHandlerBase extends DefaultExchangeHandler{
    protected final AggregationStrategy aggregationStrategy;

    public AggregatingExchangeHandlerBase(Exchange original, OptionalAsyncResultHandler optionalAsyncResultHandler, DoneStrategy<Exchange> doneStrategy, AggregationStrategy aggregationStrategy) {
        super(original, optionalAsyncResultHandler, doneStrategy);
        this.aggregationStrategy = aggregationStrategy;
    }




    protected void doAggregationComplete(Exchange exchange) {
        if (aggregationStrategy == null) {
            return;
        }
        if (aggregationStrategy instanceof CompletionAwareAggregationStrategy) {
            logger.info("Done. Finish aggregation.");
            ((CompletionAwareAggregationStrategy)aggregationStrategy).onCompletion(exchange);
        }
    }

    protected Exchange doAggregate(final Exchange oldExchange, Exchange exchange) {
        if (aggregationStrategy == null) {
            return exchange;
        }
        ExchangeHelper.prepareAggregation(oldExchange, exchange);
        final Exchange aggregatedExchange = aggregationStrategy.aggregate(oldExchange, exchange);
        logger.debug("[{}] : {}", aggregationStrategy.getClass().getCanonicalName(), exchange);
        return aggregatedExchange;
    }
}
