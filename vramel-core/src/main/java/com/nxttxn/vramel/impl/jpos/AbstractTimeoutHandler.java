package com.nxttxn.vramel.impl.jpos;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultFutureResult;

import java.util.concurrent.atomic.AtomicInteger;

/**
* Created with IntelliJ IDEA.
* User: chuck
* Date: 8/2/13
* Time: 8:27 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class AbstractTimeoutHandler<T> implements Handler<Long> {
    private final AtomicInteger counter = new AtomicInteger(0);
    protected final AsyncResultHandler<T> asyncResultHandler;
    private final int iterations;
    private Vertx vertx;

    public AbstractTimeoutHandler(AsyncResultHandler<T> asyncResultHandler, int iterations, Vertx vertx) {
        this.asyncResultHandler = asyncResultHandler;
        this.iterations = iterations;
        this.vertx = vertx;
    }

    @Override
    public void handle(Long timerId) {
        final int currentCount = counter.incrementAndGet();

        final boolean resultReceived = resultReceived();
        final boolean isTimedOut = currentCount == iterations;

        if (resultReceived || isTimedOut) {
            vertx.cancelTimer(timerId);
        }

        if (isTimedOut) {
            asyncResultHandler.handle(new DefaultFutureResult<T>(new RuntimeException("Timeout waiting for JPOS response")));
        }
    }

    protected abstract boolean resultReceived();


}
