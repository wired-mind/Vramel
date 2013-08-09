package com.nxttxn.vramel.processor.aggregate;

import com.nxttxn.vramel.Exchange;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class KeyedBodyAggregationStrategy extends AbstractKeyedAggregationStrategy<Object> {
    @Override
    public Object getValue(Exchange exchange) {
        return exchange.getIn().getBody();
    }
}
