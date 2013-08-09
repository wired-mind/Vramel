package com.nxttxn.vramel.processor.aggregate;

import com.nxttxn.vramel.Exchange;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/20/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class UseLatestAggregationStrategy implements AggregationStrategy {
    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (newExchange == null) {
            return oldExchange;
        }
        if (oldExchange == null) {
            return newExchange;
        }

        return newExchange;
    }
}
