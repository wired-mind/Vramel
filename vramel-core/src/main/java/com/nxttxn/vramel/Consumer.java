package com.nxttxn.vramel;

import org.vertx.java.core.Handler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/13/13
 * Time: 4:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Consumer extends Service {
    Endpoint getEndpoint();
}
