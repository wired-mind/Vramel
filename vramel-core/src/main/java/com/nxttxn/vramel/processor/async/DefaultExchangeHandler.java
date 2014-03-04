package com.nxttxn.vramel.processor.async;

import com.google.common.base.Optional;
import com.nxttxn.vramel.Exchange;
import org.vertx.java.core.AsyncResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
* Created with IntelliJ IDEA.
* User: chuck
* Date: 6/27/13
* Time: 2:55 PM
* To change this template use File | Settings | File Templates.
*/
public class DefaultExchangeHandler extends AbstractHandler {


    protected final Exchange original;

    public DefaultExchangeHandler(Exchange original, OptionalAsyncResultHandler optionalAsyncResultHandler, DoneStrategy<Exchange> doneStrategy) {
        super(optionalAsyncResultHandler, doneStrategy);
        this.original = original;
    }

    public DefaultExchangeHandler(Exchange original, OptionalAsyncResultHandler optionalAsyncResultHandler) {
        super(optionalAsyncResultHandler, new FixedSizeDoneStrategy<Exchange>(1, new AtomicInteger(0)));
        this.original = original;
    }

    @Override
    protected void proceed(Optional<Exchange> currentResult) throws Exception {

    }

    @Override
    protected void failed(Optional<Exchange> currentResult) {
        original.setException(currentResult.get().getException());
    }


    @Override
    protected Optional<Exchange> getFinalResult(Optional<Exchange> finalResult) {
        return finalResult;
    }

    @Override
    protected Optional<Exchange> processResult(Optional<Exchange> currentResult) {
        return currentResult;
    }

    @Override
    protected String getName() {
        return "Default Exchange Handler";
    }


}
