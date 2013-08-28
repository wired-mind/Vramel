package com.nxttxn.vramel.processor;

import com.google.common.base.Optional;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Navigate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.*;
import com.nxttxn.vramel.processor.aggregate.AggregationStrategy;
import com.nxttxn.vramel.support.AggregationSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;
import com.nxttxn.vramel.util.ServiceHelper;

import java.util.ArrayList;
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
public class MulticastProcessor extends AggregationSupport implements AsyncProcessor, Navigate<Processor> {
    public List<Processor> getProcessors() {
        return processors;
    }


    private final List<Processor> processors;
    private final boolean parallelProcessing;
    private final boolean streaming;
    private final boolean stopOnException;
    private final long timeout;
    protected final Processor onPrepare;
    private final boolean shareUnitOfWork;

    public MulticastProcessor(List<Processor> processors, Boolean parallelProcessing, AggregationStrategy aggregationStrategy, boolean streaming, boolean stopOnException, long timeout, Processor onPrepare, boolean shareUnitOfWork) {
        super(aggregationStrategy);
        this.processors = processors;
        this.parallelProcessing = parallelProcessing;
        this.streaming = streaming;
        this.stopOnException = stopOnException;
        this.timeout = timeout;
        this.onPrepare = onPrepare;
        this.shareUnitOfWork = shareUnitOfWork;
    }

    public MulticastProcessor(List<Processor> processors) {
        this(processors, false, null, false, false, 0, null, false);
    }

    /**
     * Is the multicast processor working in streaming mode?
     * <p/>
     * In streaming mode:
     * <ul>
     * <li>we use {@link Iterable} to ensure we can send messages as soon as the data becomes available</li>
     * <li>for parallel processing, we start aggregating responses as they get send back to the processor;
     * this means the {@link org.apache.camel.processor.aggregate.AggregationStrategy} has to take care of handling out-of-order arrival of exchanges</li>
     * </ul>
     */
    public boolean isStreaming() {
        return streaming;
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

        final Iterable<ProcessorExchangePair<Processor>> pairs = createProcessorExchangePairs(exchange);

        if (parallelProcessing) {
            doParallel(exchange, result, pairs, optionalAsyncResultHandler);
        } else {
            final Iterator<ProcessorExchangePair<Processor>> iterator = pairs.iterator();
            doSequential(exchange, result, optionalAsyncResultHandler, iterator, iterator.next());
        }

        return false;
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
            AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
            ap.process(newExchange, new ParallelResultHandler(optionalAsyncResultHandler, counter, result, original, aggregationStrategy));
        }
    }




    //recursive with aggregation
    private void doSequential(final Exchange original, final AtomicExchange result, final OptionalAsyncResultHandler optionalAsyncResultHandler, final Iterator<ProcessorExchangePair<Processor>> pairs, ProcessorExchangePair pair) throws Exception {
        final Exchange exchange = pair.getExchange();
        final Processor processor = pair.getProcessor();

        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
        ap.process(exchange, new SequentialResultHandler(optionalAsyncResultHandler, result, pairs, original, aggregationStrategy));

    }

    public boolean isParallelProcessing() {
        return parallelProcessing;
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

    protected void doStart() throws Exception {
        ServiceHelper.startServices(aggregationStrategy, processors);
    }



    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopServices(processors, aggregationStrategy);
    }

    @Override
    protected void doShutdown() throws Exception {
        ServiceHelper.stopAndShutdownServices(processors, aggregationStrategy);

    }

    public List<Processor> next() {
        if (!hasNext()) {
            return null;
        }
        return new ArrayList<Processor>(processors);
    }

    public boolean hasNext() {
        return processors != null && !processors.isEmpty();
    }
}
