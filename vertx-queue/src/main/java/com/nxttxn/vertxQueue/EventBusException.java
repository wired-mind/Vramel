package com.nxttxn.vertxQueue;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 5/21/13
 * Time: 12:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class EventBusException extends Exception {
    public EventBusException(String message, Exception exception) {
        super(message, exception);
    }

    public EventBusException(String message) {
        super(message);
    }
}
