package com.nxttxn.vramel;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Future;
import org.vertx.java.core.Handler;

/**
 * Created by chuck on 10/24/14.
 */
public class CountDownFutureResult<T> implements Future<T> {
    private final int size;
    private boolean failed;
    private int succeededCount;
    private Handler<AsyncResult<T>> handler;
    private T result;
    private Throwable throwable;


    public CountDownFutureResult(int size) {
        this.size = size;
    }
    /**
     * The result of the operation. This will be null if the operation failed.
     */
    public T result() {
        return result;
    }

    /**
     * An exception describing failure. This will be null if the operation succeeded.
     */
    public Throwable cause() {
        return throwable;
    }

    /**
     * Did it succeeed?
     */
    public boolean succeeded() {
        return succeededCount == size;
    }

    /**
     * Did it fail?
     */
    public boolean failed() {
        return failed;
    }

    /**
     * Has it completed?
     */
    public boolean complete() {
        return failed || succeeded();
    }

    /**
     * Set a handler for the result. It will get called when it's complete
     */
    public CountDownFutureResult<T> setHandler(Handler<AsyncResult<T>> handler) {
        this.handler = handler;
        checkCallHandler();
        return this;
    }

    /**
     * Set the result. Any handler will be called, if there is one
     */
    public CountDownFutureResult<T> setResult(T result) {
        this.result = result;
        succeededCount++;
        checkCallHandler();
        return this;
    }

    /**
     * Set the failure. Any handler will be called, if there is one
     */
    public CountDownFutureResult<T> setFailure(Throwable throwable) {
        this.throwable = throwable;
        failed = true;
        checkCallHandler();
        return this;
    }

    private void checkCallHandler() {
        if (handler != null && complete()) {
            handler.handle(this);
        }
    }
}
