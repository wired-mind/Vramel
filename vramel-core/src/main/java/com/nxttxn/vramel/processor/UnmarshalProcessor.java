package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.IOHelper;
import com.nxttxn.vramel.util.ServiceHelper;

import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnmarshalProcessor extends ServiceSupport implements Processor, VramelContextAware {
    private VramelContext vramelContext;
    private final DataFormat dataFormat;

    public UnmarshalProcessor(DataFormat dataFormat) {
        this.dataFormat = dataFormat;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        checkNotNull(dataFormat);


        final InputStream stream = exchange.getIn().getMandatoryBody(InputStream.class);

        try {
            // lets setup the out message before we invoke the dataFormat so that it can mutate it if necessary
            Message out = exchange.getOut();
            out.copyFrom(exchange.getIn());

            Object result = dataFormat.unmarshal(exchange, stream);
            out.setBody(result);
        } catch (Exception e) {
            // remove OUT message, as an exception occurred
            exchange.setOut(null);
            throw e;
        } finally {
            IOHelper.close(stream, "input stream");
        }
    }

    public String toString() {
        return "Unmarshal[" + dataFormat + "]";
    }

    public String getTraceLabel() {
        return "unmarshal[" + dataFormat + "]";
    }

    public VramelContext getVramelContext() {
        return vramelContext;
    }

    public void setVramelContext(VramelContext vramelContext) {
        this.vramelContext = vramelContext;
    }

    @Override
    protected void doStart() throws Exception {
        // inject CamelContext on data format
        if (dataFormat instanceof VramelContextAware) {
            ((VramelContextAware) dataFormat).setVramelContext(vramelContext);
        }
        ServiceHelper.startService(dataFormat);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(dataFormat);
    }
}
