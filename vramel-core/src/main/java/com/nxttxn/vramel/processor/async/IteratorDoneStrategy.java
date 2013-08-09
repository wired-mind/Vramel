package com.nxttxn.vramel.processor.async;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/28/13
 * Time: 12:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class IteratorDoneStrategy<T> implements DoneStrategy<T> {
    private final Iterator iterator;

    public IteratorDoneStrategy(Iterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean isDone(T t) {
        return !iterator.hasNext();
    }
}
