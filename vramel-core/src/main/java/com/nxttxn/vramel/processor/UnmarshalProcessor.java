package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Message;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.util.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnmarshalProcessor implements Processor {
    private final DataFormat dataFormat;

    public UnmarshalProcessor(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public void process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception{
        checkNotNull(dataFormat);

        //Very simplistic right now. Assume that body is a byte[].
        final byte[] body = exchange.getIn().getBody(byte[].class);
        InputStream stream = new ByteArrayInputStream(body);
        try {
            // lets setup the out message before we invoke the dataFormat so that it can mutate it if necessary
            Message out = exchange.getOut();
            out.copyFrom(exchange.getIn());

            Object result = dataFormat.unmarshal(exchange, stream);
            out.setBody(result);
            optionalAsyncResultHandler.done(exchange);

        } catch (Exception e) {
            // remove OUT message, as an exception occurred
            exchange.setOut(null);
            throw e;
        } finally {
            IOHelper.close(stream, "input stream");
        }
    }
}
