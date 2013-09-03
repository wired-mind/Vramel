package com.nxttxn.vramel.util;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.ChoiceProcessor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;
import com.nxttxn.vramel.spi.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/14/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AsyncProcessorHelper {
    private static final transient Logger LOG = LoggerFactory.getLogger(AsyncProcessorHelper.class);


    /**
     * Calls the async version of the processor's process method.
     * <p/>
     * This implementation supports transacted {@link Exchange}s which ensure those are run in a synchronous fashion.
     * See more details at {@link org.apache.camel.AsyncProcessor}.
     *
     * @param processor the processor
     * @param exchange  the exchange
     * @param optionalAsyncResultHandler  the callback
     * @return <tt>true</tt> to continue execute synchronously, <tt>false</tt> to continue being executed asynchronously
     */
    public static boolean process(final AsyncProcessor processor, final Exchange exchange, final OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception {
        boolean sync;

//        if (exchange.isTransacted()) {
//            // must be synchronized for transacted exchanges
//            LOG.trace("Transacted Exchange must be routed synchronously for exchangeId: {} -> {}", exchange.getExchangeId(), exchange);
//            try {
//                process(processor, exchange);
//            } catch (Throwable e) {
//                exchange.setException(e);
//            }
//            callback.done(true);
//            sync = true;
//        } else {

            final UnitOfWork uow = exchange.getUnitOfWork();

            // allow unit of work to wrap callback in case it need to do some special work
            // for example the MDCUnitOfWork
            OptionalAsyncResultHandler async = optionalAsyncResultHandler;
            if (uow != null) {
                async = uow.beforeProcess(processor, exchange, optionalAsyncResultHandler);
            }

            // we support asynchronous routing so invoke it
            sync = processor.process(exchange, async);

            // execute any after processor work (in current thread, not in the callback)
            if (uow != null) {
                uow.afterProcess(processor, exchange, optionalAsyncResultHandler, sync);
            }
//        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Exchange processed and is continued routed {} for exchangeId: {} -> {}",
                    new Object[]{sync ? "synchronously" : "asynchronously", exchange.getExchangeId(), exchange});
        }
        return sync;
    }
    //This method should not be used, but we will have it here for symmetry.
    public static void process(AsyncProcessor asyncProcessor, Exchange exchange) {
        throw new UnsupportedOperationException("Vramel doesn't allow async processors to run syncronously.");
    }
}
