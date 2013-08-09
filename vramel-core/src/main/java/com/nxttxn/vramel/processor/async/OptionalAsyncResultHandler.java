package com.nxttxn.vramel.processor.async;

import com.google.common.base.Optional;
import com.nxttxn.vramel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/6/13
 * Time: 5:00 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class OptionalAsyncResultHandler implements AsyncExchangeResultHandler {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());


    public void done(Exchange response) {
        this.handle(new AsyncExchangeResult(response));
    }
}
