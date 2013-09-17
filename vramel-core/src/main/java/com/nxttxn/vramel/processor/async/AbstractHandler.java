package com.nxttxn.vramel.processor.async;

import com.google.common.base.Optional;

import com.nxttxn.vramel.Exchange;
import org.vertx.java.core.AsyncResult;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/27/13
 * Time: 2:57 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractHandler extends OptionalAsyncResultHandler{

    protected final OptionalAsyncResultHandler optionalAsyncResultHandler;
    private final DoneStrategy<Exchange> doneStrategy;

    public AbstractHandler(OptionalAsyncResultHandler optionalAsyncResultHandler, DoneStrategy<Exchange> doneStrategy) {
        this.optionalAsyncResultHandler = optionalAsyncResultHandler;
        this.doneStrategy = doneStrategy;
    }

    @Override
    public void handle(AsyncExchangeResult asyncExchangeResult) {
        final Optional<Exchange> result = asyncExchangeResult.result;
        try {
            logger.info("{} completed", getName());
            if (asyncExchangeResult.failed()) {
                failed(result);
                optionalAsyncResultHandler.done(result.get());
                return;
            }


            logger.debug(String.format("[%s] Result: %s", getName(), result.get().toString()));
            Optional<Exchange> currentResult = processResult(result);
            final Exchange nextExchange = createNextExchange(currentResult.get());
            logger.debug(String.format("[%s] Processed Result: %s", getName(), nextExchange.toString()));
            if (doneStrategy.isDone(nextExchange)) {
                Optional<Exchange> finalResult = getFinalResult(Optional.of(nextExchange));
                logger.debug(String.format("[%s] Final Result: %s", getName(), finalResult.get().toString()));
                optionalAsyncResultHandler.done(finalResult.get());
            } else {
                proceed(Optional.of(nextExchange));
            }
        } catch (Exception e) {
            result.get().setException(e);
            optionalAsyncResultHandler.done(result.get());
        }
    }


    protected abstract void proceed(Optional<Exchange> currentResult) throws Exception;

    protected abstract void failed(Optional<Exchange> currentResult);

    protected abstract Optional<Exchange> getFinalResult(Optional<Exchange> finalResult);

    protected abstract Optional<Exchange> processResult(Optional<Exchange> currentResult);

    protected abstract String getName();

    protected Exchange createNextExchange(Exchange previousExchange) {
        Exchange answer = previousExchange;

        // now lets set the input of the next exchange to the output of the
        // previous message if it is not null
        if (answer.hasOut()) {
            answer.setIn(answer.getOut());
            answer.setOut(null);
        }
        return answer;
    }
}
