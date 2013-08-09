package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.Producer;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/24/13
 * Time: 11:10 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ProcessorExchangePair<T extends Processor> {

    int getIndex();

    Exchange getExchange();

    Producer getProducer();

    T getProcessor();

    void begin();

    void done();

}
