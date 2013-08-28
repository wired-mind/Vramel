package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.*;
import com.nxttxn.vramel.processor.async.AsyncExchangeResult;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 2:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DelegateAsyncProcessor extends ServiceSupport implements DelegateProcessor, AsyncProcessor, Navigate<Processor> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected AsyncProcessor processor;

    public DelegateAsyncProcessor(AsyncProcessor processor) {
        if (processor == this) {
            throw new IllegalArgumentException("Recursive DelegateAsyncProcessor!");
        }
        this.processor = processor;
    }
    public DelegateAsyncProcessor(Processor processor) {
        this(AsyncProcessorConverterHelper.convert(processor));
    }

    @Override
    public String toString() {
        return "DelegateAsync[" + processor + "]";
    }

    public AsyncProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(AsyncProcessor processor) {
        this.processor = processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = AsyncProcessorConverterHelper.convert(processor);
    }

    protected void doStart() throws Exception {
        ServiceHelper.startServices(processor);
    }

    protected void doStop() throws Exception {
        ServiceHelper.stopServices(processor);
    }

    public void process(Exchange exchange) throws Exception {
        if (getProcessor() == null) {
            return;
        }

        getProcessor().process(exchange, new OptionalAsyncResultHandler() {
            @Override
            public void handle(AsyncExchangeResult event) {
                logger.warn("Called synchronous process");
            }
        });

        return;
    }

    public boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        return processNext(exchange, optionalAsyncResultHandler);
    }

    protected boolean processNext(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        if (getProcessor() == null) {
            // no processor then we are done
            optionalAsyncResultHandler.done(exchange);
            return true;
        }
        return getProcessor().process(exchange, optionalAsyncResultHandler);
    }

    public boolean hasNext() {
        return processor != null;
    }

    public List<Processor> next() {
        if (!hasNext()) {
            return null;
        }
        List<Processor> answer = new ArrayList<Processor>(1);
        answer.add(processor);
        return answer;
    }
}
