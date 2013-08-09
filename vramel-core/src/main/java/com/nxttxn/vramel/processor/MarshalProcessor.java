package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.spi.DataFormat;

import java.io.ByteArrayOutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarshalProcessor implements Processor {
    private final DataFormat dataFormat;

    public MarshalProcessor(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        checkNotNull(dataFormat);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Message in = exchange.getIn();
        Object body = in.getBody();

        // lets setup the out message before we invoke the dataFormat
        // so that it can mutate it if necessary
        Message out = exchange.getOut();
        out.copyFrom(in);

        try {
            dataFormat.marshal(exchange, body, buffer);
            byte[] data = buffer.toByteArray();
            out.setBody(data);
            optionalAsyncResultHandler.done(exchange);
        } catch (Exception e) {
            // remove OUT message, as an exception occurred
            exchange.setOut(null);
            throw e;
        }
    }
}
