package com.nxttxn.vramel.processor.aggregate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.impl.DefaultExchange;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import org.vertx.java.core.AsyncResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/20/13
 * Time: 1:43 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractKeyedAggregationStrategy<V> implements AggregationStrategy {
    public static final String KEY = "AggregationKey";


    /**
     * This method is implemented by the sub-class and is called to retrieve
     * an instance of the value that will be aggregated and forwarded to the
     * receiving end point.
     * <p/>
     * If <tt>null</tt> is returned, then the value is <b>not</b> added to the {@link List}.
     *
     * @param exchange The exchange that is used to retrieve the value from
     * @return An instance of V that is the associated value of the passed exchange
     */
    public abstract V getValue(Exchange exchange);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        KeyedAggregation results;

        Exchange answer = oldExchange;

        if (oldExchange == null) {
            answer = new DefaultExchange(newExchange);
            results = getResults(answer);
        } else {
            results = getResults(oldExchange);
        }

        if (newExchange != null) {
            addValue(results, newExchange);
        }

        final Message out = answer.getOut();
        out.copyFrom(answer.getIn());
        out.setBody(results);

        return answer;

    }

    private void addValue(KeyedAggregation results, Exchange exchange) {

        V value = getValue(exchange);

        if (value != null) {
            String key = exchange.getProperty(KEY, String.class);
            if (key == null) {
                key = value.getClass().getCanonicalName();
            }
            results.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private KeyedAggregation getResults(Exchange exchange) {
        KeyedAggregation results;

        results = exchange.getIn().getBody(KeyedAggregation.class);
        if (results == null) {
            results = new KeyedAggregation();
            addValue(results, exchange);
        }

        return results;
    }

}
