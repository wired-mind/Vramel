package com.nxttxn.vramel.processor.async;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/27/13
 * Time: 11:46 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DoneStrategy<T> {
    boolean isDone(T t);
}
