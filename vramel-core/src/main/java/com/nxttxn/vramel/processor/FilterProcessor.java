package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterProcessor extends DelegateProcessor {

    private final com.nxttxn.vramel.Predicate predicate;

    public FilterProcessor(Predicate predicate, Processor processor) {
        super(processor);
        this.predicate = predicate;
    }

    @Override
    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        boolean isMatched = false;
        try {
            isMatched = getPredicate().matches(exchange);
        } catch (Exception e) {
            exchange.setException(e);
            optionalAsyncResultHandler.done(exchange);
        }

        logger.debug("Filter matches: {} for exchange: {}", isMatched, exchange.toString());

        if (isMatched) {
            super.process(exchange, optionalAsyncResultHandler);
        } else {
            optionalAsyncResultHandler.done(exchange);
        }
    }


    public Predicate getPredicate() {
        return predicate;
    }
}
