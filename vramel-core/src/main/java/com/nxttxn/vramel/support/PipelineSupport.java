package com.nxttxn.vramel.support;

import com.google.common.base.Optional;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.processor.ProcessorExchangePair;
import com.nxttxn.vramel.processor.async.DefaultExchangeHandler;
import com.nxttxn.vramel.processor.async.IteratorDoneStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.nxttxn.vramel.processor.PipelineHelper.continueProcessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/27/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PipelineSupport extends ServiceSupport {
    protected final Logger logger = LoggerFactory.getLogger(getClass());


    //basic recursive pipeline
    protected boolean process(final Exchange original, final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler, final Iterator<Processor> processors, Processor processor) throws Exception {
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
        return ap.process(exchange, new PipelineResultsHandler(optionalAsyncResultHandler, processors, exchange, original));
    }

    protected <T extends Processor> Iterable<ProcessorExchangePair<T>> createProcessorExchangePairs(Exchange original, List<T> processorList) {
        List<ProcessorExchangePair<T>> result = new ArrayList<>(processorList.size());

        int index = 0;
        for (T processor : processorList) {
            Exchange copy = original.copy();
            result.add(new DefaultProcessorExchangePair<T>(index++, processor, copy));
        }

        return result;
    }

    /**
     * Class that represent each step in the multicast route to do
     */
    static final class DefaultProcessorExchangePair<T extends Processor> implements ProcessorExchangePair<T> {
        private final int index;
        private final T processor;
        private final Exchange exchange;

        private DefaultProcessorExchangePair(int index, T processor, Exchange exchange) {
            this.index = index;
            this.processor = processor;
            this.exchange = exchange;
        }

        public int getIndex() {
            return index;
        }

        public Exchange getExchange() {
            return exchange;
        }

        public Producer getProducer() {
            if (processor instanceof Producer) {
                return (Producer) processor;
            }
            return null;
        }

        public T getProcessor() {
            return processor;
        }

        public void begin() {
            // noop
        }

        public void done() {
            // noop
        }

    }

    private class PipelineResultsHandler extends DefaultExchangeHandler {
        private final Iterator<Processor> processors;
        private final Exchange exchange;
        private final Exchange original;

        public PipelineResultsHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, Iterator<Processor> processors, Exchange exchange, Exchange original) {
            super(original, optionalAsyncResultHandler, new IteratorDoneStrategy(processors));
            this.processors = processors;
            this.exchange = exchange;
            this.original = original;
        }

        @Override
        protected void proceed(Optional<Exchange> currentResult) throws Exception {
            // check for error if so we should break out
            if (!continueProcessing(exchange, "so breaking out of pipeline", logger)) {
                optionalAsyncResultHandler.done(exchange);
                return;
            }

            // continue processing the pipeline asynchronously
            Exchange nextExchange = createNextExchange(currentResult.or(exchange));


            final Processor next = processors.next();

            logger.debug(String.format("Ready to process next processor using exchange: %s", nextExchange.toString()));
            process(original, nextExchange, optionalAsyncResultHandler, processors, next);
        }

        @Override
        protected Optional<Exchange> getFinalResult(Optional<Exchange> finalResult) {
            copyResults(original, finalResult.get());
            return Optional.of(original);
        }


        @Override
        protected String getName() {
            return "Pipeline result handler";
        }


    }


}
