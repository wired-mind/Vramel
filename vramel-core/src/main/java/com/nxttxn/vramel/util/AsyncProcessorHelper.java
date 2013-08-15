package com.nxttxn.vramel.util;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.processor.ChoiceProcessor;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 8/14/13
 * Time: 6:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AsyncProcessorHelper {

    //This method should not be used, but we will have it here for symmetry.
    public static void process(AsyncProcessor asyncProcessor, Exchange exchange) {
        throw new UnsupportedOperationException("Vramel doesn't allow async processors to run syncronously.");
    }
}
