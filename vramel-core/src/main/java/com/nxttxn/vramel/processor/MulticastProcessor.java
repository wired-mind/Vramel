package com.nxttxn.vramel.processor;

import com.google.common.base.Optional;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Navigate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.processor.async.*;
import com.nxttxn.vramel.support.MulticastSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/20/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class MulticastProcessor extends MulticastSupport implements AsyncProcessor, Navigate<Processor> {

    public MulticastProcessor(Collection<Processor> processors, AggregationStrategy aggregationStrategy, Boolean parallelProcessing, boolean streaming, boolean stopOnException, long timeout, Processor onPrepare, boolean shareUnitOfWork) {
        super(processors, aggregationStrategy, parallelProcessing, streaming, stopOnException, timeout, onPrepare, shareUnitOfWork);
    }

    public MulticastProcessor(List<Processor> processors) {
        super(processors);
    }

    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final AtomicExchange result = new AtomicExchange();

        if (isEmpty()) {
            optionalAsyncResultHandler.done(exchange);
            return false;
        }

        final Iterable<ProcessorExchangePair> pairs = createProcessorExchangePairs(exchange);

        if (isParallelProcessing()) {
            doParallel(exchange, result, pairs, optionalAsyncResultHandler);
        } else {
            final Iterator<ProcessorExchangePair> iterator = pairs.iterator();
            doSequential(exchange, result, optionalAsyncResultHandler, iterator, iterator.next());
        }

        return false;
    }

    private boolean isEmpty() {
        return getProcessors().size() == 0;
    }

    protected Iterable<ProcessorExchangePair> createProcessorExchangePairs(Exchange original) throws Exception{
        return createProcessorExchangePairs(original, getProcessors());
    }

    private void doParallel(final Exchange original, final AtomicExchange result, Iterable<ProcessorExchangePair> pairs, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);

        for (ProcessorExchangePair pair : pairs) {
            Exchange newExchange = pair.getExchange();
            final Processor processor = pair.getProcessor();
            AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
            ap.process(newExchange, new ParallelResultHandler(optionalAsyncResultHandler, counter, result, original, getAggregationStrategy()));
        }
    }




    //recursive with aggregation
    private void doSequential(final Exchange original, final AtomicExchange result, final OptionalAsyncResultHandler optionalAsyncResultHandler, final Iterator<ProcessorExchangePair> pairs, ProcessorExchangePair pair) throws Exception {
        final Exchange exchange = pair.getExchange();
        final Processor processor = pair.getProcessor();

        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
        ap.process(exchange, new SequentialResultHandler(optionalAsyncResultHandler, result, pairs, original, getAggregationStrategy()));

    }


    private class ParallelResultHandler extends AggregatingExchangeHandler {

        public ParallelResultHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, AtomicInteger counter, AtomicExchange result, Exchange original, AggregationStrategy aggregationStrategy) {
            super(original, optionalAsyncResultHandler, new FixedSizeDoneStrategy(getProcessors().size(), counter), aggregationStrategy, result);
        }

        @Override
        protected String getName() {
            return "Parallel Results Handler";
        }
    }

    private class SequentialResultHandler extends AggregatingExchangeHandler {

        private final Iterator<ProcessorExchangePair> pairs;


        public SequentialResultHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, AtomicExchange result, Iterator<ProcessorExchangePair> pairs, Exchange original, AggregationStrategy aggregationStrategy) {
            super(original, optionalAsyncResultHandler, new IteratorDoneStrategy(pairs), aggregationStrategy, result);

            this.pairs = pairs;

        }

        @Override
        protected String getName() {
            return "Sequential Results Handler";
        }

        @Override
        protected void proceed(Optional<Exchange> currentResult) throws Exception {
            final ProcessorExchangePair next = pairs.next();
            doSequential(original, result, optionalAsyncResultHandler, pairs, next);
        }
    }


    public List<Processor> next() {
        if (!hasNext()) {
            return null;
        }
        return new ArrayList<Processor>(getProcessors());
    }

    public boolean hasNext() {
        return getProcessors() != null && !getProcessors().isEmpty();
    }



    protected Integer getExchangeIndex(Exchange exchange) {
        return exchange.getProperty(Exchange.MULTICAST_INDEX, Integer.class);
    }

    protected void updateNewExchange(Exchange exchange, int index, Iterable<ProcessorExchangePair> allPairs,
                                     Iterator<ProcessorExchangePair> it) {
        exchange.setProperty(Exchange.MULTICAST_INDEX, index);
        if (it.hasNext()) {
            exchange.setProperty(Exchange.MULTICAST_COMPLETE, Boolean.FALSE);
        } else {
            exchange.setProperty(Exchange.MULTICAST_COMPLETE, Boolean.TRUE);
        }
    }

}
