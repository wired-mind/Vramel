package com.nxttxn.vramel;

import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 11:03 AM
 * To change this template use File | Settings | File Templates.
 */
public interface AsyncProcessor extends Processor {
    boolean process(Exchange exchange, OptionalAsyncResultHandler optionalAsyncResultHandler) throws Exception;
}
