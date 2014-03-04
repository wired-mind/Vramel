package com.nxttxn.vramel.support;

import com.google.common.base.Optional;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.DefaultExchangeHandler;
import com.nxttxn.vramel.processor.async.IteratorDoneStrategy;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.ExchangeHelper;

import java.util.Iterator;

import static com.nxttxn.vramel.processor.PipelineHelper.continueProcessing;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/28/13
 * Time: 11:08 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class PipelineSupport extends ServiceSupport {
    //basic recursive pipeline
    protected boolean process(final Exchange original, final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler, final Iterator<Processor> processors, Processor processor) throws Exception {
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(processor);
        return ap.process(exchange, new PipelineResultsHandler(optionalAsyncResultHandler, processors, exchange, original));
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
            if (!continueProcessing(currentResult.or(exchange), "so breaking out of pipeline", logger)) {
                optionalAsyncResultHandler.done(currentResult.or(exchange));
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
            ExchangeHelper.copyResults(original, finalResult.get());
            return Optional.of(original);
        }


        @Override
        protected String getName() {
            return "Pipeline result handler";
        }


    }
}
