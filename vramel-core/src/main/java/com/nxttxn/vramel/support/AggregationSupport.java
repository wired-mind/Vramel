package com.nxttxn.vramel.support;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.CompletionAwareAggregationStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.CastUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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


    /**
     * Use {@link #getAggregationStrategy(Exchange)} instead.
     */
    public AggregationStrategy getAggregationStrategy() {
        return aggregationStrategy;
    }

    /**
     * Sets the given {@link org.apache.camel.processor.aggregate.AggregationStrategy} on the {@link Exchange}.
     *
     * @param exchange            the exchange
     * @param aggregationStrategy the strategy
     */
    protected void setAggregationStrategyOnExchange(Exchange exchange, AggregationStrategy aggregationStrategy) {
        Map<?, ?> property = exchange.getProperty(Exchange.AGGREGATION_STRATEGY, Map.class);
        Map<Object, AggregationStrategy> map = CastUtils.cast(property);
        if (map == null) {
            map = new HashMap<Object, AggregationStrategy>();
        } else {
            // it is not safe to use the map directly as the exchange doesn't have the deep copy of it's properties
            // we just create a new copy if we need to change the map
            map = new HashMap<Object, AggregationStrategy>(map);
        }
        // store the strategy using this processor as the key
        // (so we can store multiple strategies on the same exchange)
        map.put(this, aggregationStrategy);
        exchange.setProperty(Exchange.AGGREGATION_STRATEGY, map);
    }

    protected AggregationStrategy getAggregationStrategy(Exchange exchange) {
        AggregationStrategy answer = null;

        // prefer to use per Exchange aggregation strategy over a global strategy
        if (exchange != null) {
            Map<?, ?> property = exchange.getProperty(Exchange.AGGREGATION_STRATEGY, Map.class);
            Map<Object, AggregationStrategy> map = CastUtils.cast(property);
            if (map != null) {
                answer = map.get(this);
            }
        }
        if (answer == null) {
            // fallback to global strategy
            answer = getAggregationStrategy();
        }
        return answer;
    }


}
