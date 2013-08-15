package com.nxttxn.vramel.model;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.LogProcessor;
import com.nxttxn.vramel.spi.FlowContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 10:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogDefinition extends NoOutputDefinition<LogDefinition> {
    private final String message;

    public LogDefinition(String message) {
        this.message = message;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        final Logger logger = LoggerFactory.getLogger("VramelLogger");

        return new LogProcessor(message, logger);
    }
}
