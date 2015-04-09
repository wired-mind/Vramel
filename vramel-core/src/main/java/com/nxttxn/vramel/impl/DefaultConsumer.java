package com.nxttxn.vramel.impl;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Consumer;
import com.nxttxn.vramel.Endpoint;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.spi.ExceptionHandler;
import com.nxttxn.vramel.support.ServiceSupport;
import com.nxttxn.vramel.util.AsyncProcessorConverterHelper;
import com.nxttxn.vramel.util.ServiceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 1:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultConsumer extends ServiceSupport implements Consumer {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final Endpoint endpoint;
    private volatile AsyncProcessor asyncProcessor;
    private final Processor processor;
    private ExceptionHandler exceptionHandler;


    public DefaultConsumer(Endpoint endpoint, Processor processor) {

        this.endpoint = endpoint;
        this.processor = processor;
    }

    @Override
    public Endpoint getEndpoint() {
        return endpoint;
    }

    protected void logError(Throwable ex) {

        logger.error(ex.getMessage(), ex);
        if (ex.getCause() != null) {
            logError(ex.getCause());
        }


    }

    protected void doStop() throws Exception {
        logger.debug("Stopping consumer: {}", this);
        ServiceHelper.stopServices(processor);
    }

    protected void doStart(AsyncResultHandler<Void> asyncResultHandler) throws Exception {
        logger.debug("Starting consumer: {}", this);
        ServiceHelper.startServices(processor);
    }
    public Processor getProcessor() {
        return processor;
    }

    /**
     * Provides an {@link org.apache.camel.AsyncProcessor} interface to the configured
     * processor on the consumer. If the processor does not implement the interface,
     * it will be adapted so that it does.
     */
    public synchronized AsyncProcessor getAsyncProcessor() {
        if (asyncProcessor == null) {
            asyncProcessor = AsyncProcessorConverterHelper.convert(processor);
        }
        return asyncProcessor;
    }

    public ExceptionHandler getExceptionHandler() {
        if (exceptionHandler == null) {
            exceptionHandler = new LoggingExceptionHandler(getClass());
        }
        return exceptionHandler;
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

}
