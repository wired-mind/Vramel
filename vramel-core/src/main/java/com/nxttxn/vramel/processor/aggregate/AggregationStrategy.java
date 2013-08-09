
package com.nxttxn.vramel.processor.aggregate;


import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

public interface AggregationStrategy {

    Exchange aggregate(Exchange oldExchange, Exchange newExchange);
}
