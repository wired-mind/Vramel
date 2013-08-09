package com.nxttxn.vramel.processor.async;

import com.google.common.base.Optional;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/28/13
 * Time: 12:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class AggregatingExchangeHandler extends AggregatingExchangeHandlerBase {
    protected final AtomicExchange result;

    public AggregatingExchangeHandler(Exchange original, OptionalAsyncResultHandler optionalAsyncResultHandler, DoneStrategy<Exchange> doneStrategy, AggregationStrategy aggregationStrategy, AtomicExchange result) {
        super(original, optionalAsyncResultHandler, doneStrategy, aggregationStrategy);
        this.result = result;
    }


    @Override
    protected Optional<Exchange> processResult(Optional<Exchange> currentResult) {
        final Exchange replyExchange = currentResult.get();
        final Exchange aggregatedExchange = doAggregate(result.get(), replyExchange);
        result.set(aggregatedExchange);
        return Optional.of(aggregatedExchange);
    }

    @Override
    protected Optional<Exchange> getFinalResult(Optional<Exchange> finalResult) {
        final Exchange subExchange = result.get();
        doAggregationComplete(subExchange);
        copyResults(original, subExchange);
        return Optional.of(original);
    }

}
