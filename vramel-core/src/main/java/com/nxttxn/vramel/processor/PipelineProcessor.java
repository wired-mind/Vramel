package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class PipelineProcessor extends MulticastProcessor {


    public PipelineProcessor(List<Processor> processors) {
        super(processors);
    }

    @Override
    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Iterator<Processor> iterator = getProcessors().iterator();
        return process(exchange, exchange.copy(), optionalAsyncResultHandler, iterator, iterator.next());
    }

}
