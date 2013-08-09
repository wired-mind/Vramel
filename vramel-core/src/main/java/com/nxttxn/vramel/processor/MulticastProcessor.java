package com.nxttxn.vramel.processor;

import com.google.common.base.Optional;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.async.*;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.support.AggregationSupport;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/20/13
 * Time: 10:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class MulticastProcessor extends AggregationSupport implements Processor {
    public List<Processor> getProcessors() {
        return processors;
    }


    private final List<Processor> processors;
    private final Boolean parallelProcessing;

    public MulticastProcessor(List<Processor> processors, Boolean parallelProcessing, AggregationStrategy aggregationStrategy) {
        super(aggregationStrategy);
        this.processors = processors;
        this.parallelProcessing = parallelProcessing;
    }

    public MulticastProcessor(List<Processor> processors) {
        this(processors, false, null);
    }

    @Override
    public void process(Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final AtomicExchange result = new AtomicExchange();

        if (isEmpty()) {
            optionalAsyncResultHandler.done(exchange);
        }

        final Iterable<ProcessorExchangePair<Processor>> pairs = createProcessorExchangePairs(exchange);

        if (parallelProcessing) {
            doParallel(exchange, result, pairs, optionalAsyncResultHandler);
        } else {
            final Iterator<ProcessorExchangePair<Processor>> iterator = pairs.iterator();
            doSequential(exchange, result, optionalAsyncResultHandler, iterator, iterator.next());
        }
    }

    private boolean isEmpty() {
        return getProcessors().size() == 0;
    }

    private Iterable<ProcessorExchangePair<Processor>> createProcessorExchangePairs(Exchange original) {
        return createProcessorExchangePairs(original, getProcessors());
    }

    private void doParallel(final Exchange original, final AtomicExchange result, Iterable<ProcessorExchangePair<Processor>> pairs, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {

        final AtomicInteger counter = new AtomicInteger(0);

        for (ProcessorExchangePair pair : pairs) {
            Exchange newExchange = pair.getExchange();
            final Processor processor = pair.getProcessor();
            processor.process(newExchange, new ParallelResultHandler(optionalAsyncResultHandler, counter, result, original, aggregationStrategy));
        }
    }




    //recursive with aggregation
    private void doSequential(final Exchange original, final AtomicExchange result, final OptionalAsyncResultHandler optionalAsyncResultHandler, final Iterator<ProcessorExchangePair<Processor>> pairs, ProcessorExchangePair pair) throws Exception {
        final Exchange exchange = pair.getExchange();
        final Processor processor = pair.getProcessor();

        processor.process(exchange, new SequentialResultHandler(optionalAsyncResultHandler, result, pairs, original, aggregationStrategy));

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

        private final Iterator<ProcessorExchangePair<Processor>> pairs;


        public SequentialResultHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, AtomicExchange result, Iterator<ProcessorExchangePair<Processor>> pairs, Exchange original, AggregationStrategy aggregationStrategy) {
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
}
