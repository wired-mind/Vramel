package com.nxttxn.vramel;

import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 10:03 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Processor {
    void process(Exchange exchange) throws Exception;
}
