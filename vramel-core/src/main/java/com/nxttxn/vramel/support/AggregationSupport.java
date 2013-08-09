package com.nxttxn.vramel.support;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.CompletionAwareAggregationStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 1:15 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AggregationSupport extends PipelineSupport {
    protected final AggregationStrategy aggregationStrategy;

    public AggregationSupport(AggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }



}
