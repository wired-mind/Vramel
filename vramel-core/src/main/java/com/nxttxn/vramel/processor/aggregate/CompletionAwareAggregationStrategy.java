package com.nxttxn.vramel.processor.aggregate;

import com.nxttxn.vramel.Exchange;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/24/13
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CompletionAwareAggregationStrategy extends AggregationStrategy {
    void onCompletion(Exchange exchange);
}
