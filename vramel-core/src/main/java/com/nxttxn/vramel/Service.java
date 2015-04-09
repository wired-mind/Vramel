package com.nxttxn.vramel;

import org.vertx.java.core.AsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/8/13
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Service {

    /**
     * Starts the service
     *
     * @throws Exception is thrown if starting failed
     * @param asyncResultHandler
     */
    void start(AsyncResultHandler<Void> asyncResultHandler) throws Exception;

    /**
     * Stops the service
     *
     * @throws Exception is thrown if stopping failed
     */
    void stop() throws Exception;

}
