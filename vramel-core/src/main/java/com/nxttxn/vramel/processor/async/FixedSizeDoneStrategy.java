package com.nxttxn.vramel.processor.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/27/13
 * Time: 11:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class FixedSizeDoneStrategy<T> implements DoneStrategy<T> {
    protected final transient Logger logger = LoggerFactory.getLogger(FixedSizeDoneStrategy.class);
    private final AtomicInteger counter;
    private final int expectedResultsCount;
    public FixedSizeDoneStrategy(int size, AtomicInteger counter) {
        this.expectedResultsCount = size;
        this.counter = counter;
    }

    public boolean isDone(T t) {
        final int resultCount = counter.incrementAndGet();

        final boolean done = resultCount == expectedResultsCount;
        if (done) {
            logger.info("Sequence finished. {} total in sequence.", resultCount);
        }

        return done;
    }
}
