package com.nxttxn.vramel;

import org.apache.camel.IsSingleton;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 8:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Producer extends Processor, IsSingleton {
    Endpoint getEndpoint();
}
