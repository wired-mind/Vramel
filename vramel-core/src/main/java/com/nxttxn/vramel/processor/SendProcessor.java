package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Producer;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.AsyncProcessorHelper;


/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendProcessor implements AsyncProcessor {
    private final Endpoint endpoint;

    public SendProcessor(Endpoint endpoint) {
        this.endpoint = endpoint;
    }


    public void process(Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    @Override
    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Producer producer = endpoint.createProducer();
        AsyncProcessor ap = AsyncProcessorConverterHelper.convert(producer);
        ap.process(exchange, optionalAsyncResultHandler);
        return false;
    }
}
