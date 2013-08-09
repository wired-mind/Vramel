package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class SendProcessor implements Processor {
    private final Endpoint endpoint;

    public SendProcessor(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        final Producer producer = endpoint.createProducer();
        producer.process(exchange, optionalAsyncResultHandler);
    }
}
