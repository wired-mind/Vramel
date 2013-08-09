package com.nxttxn.vramel.processor.aggregate;

import com.nxttxn.vramel.Exchange;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/24/13
 * Time: 12:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyedExchangeAggregationStrategy extends AbstractKeyedAggregationStrategy<Exchange> {
    @Override
    public Exchange getValue(Exchange exchange) {
        return exchange;
    }
}
