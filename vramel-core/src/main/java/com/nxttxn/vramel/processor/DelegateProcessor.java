package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 2:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelegateProcessor implements Processor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Processor processor;

    public DelegateProcessor(Processor processor) {
        this.processor = processor;
    }

    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        processNext(exchange, optionalAsyncResultHandler);
    }

    protected void processNext(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        if (getProcessor() == null) {
            // no processor then we are done
            optionalAsyncResultHandler.done(exchange);
            return;
        }
        getProcessor().process(exchange, optionalAsyncResultHandler);
    }

    public Processor getProcessor() {
        return processor;
    }
}
