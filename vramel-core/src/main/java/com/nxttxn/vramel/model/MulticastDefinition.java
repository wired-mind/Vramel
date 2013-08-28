package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.MulticastProcessor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.aggregate.UseLatestAggregationStrategy;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 9:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class MulticastDefinition extends OutputDefinition<MulticastDefinition> {

    private Boolean parallelProcessing;
    private AggregationStrategy aggregationStrategy;

    public Boolean getParallelProcessing() {
        return parallelProcessing;
    }

    public void setParallelProcessing(Boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
    }

    public MulticastDefinition parallelProcessing() {
        setParallelProcessing(true);
        return this;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        return createChildProcessor(flowContext);
    }

    @Override
    protected Processor createCompositeProcessor(FlowContext flowContext, List<Processor> list) throws Exception {
        if (getAggregationStrategy() == null) {
            // default to use latest aggregation strategy
            setAggregationStrategy(new UseLatestAggregationStrategy());
        }
        return new MulticastProcessor(list, getParallelProcessing(), getAggregationStrategy(), streaming, stopOnException, timeout, onPrepare, shareUnitOfWork);
    }

    public AggregationStrategy getAggregationStrategy() {
        return aggregationStrategy;
    }

    public void setAggregationStrategy(AggregationStrategy aggregationStrategy) {
        this.aggregationStrategy = aggregationStrategy;
    }

    public boolean isShareUnitOfWork() {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }
}
