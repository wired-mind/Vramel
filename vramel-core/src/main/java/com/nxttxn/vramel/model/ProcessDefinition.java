package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.impl.DefaultVramelContext;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProcessDefinition extends NoOutputDefinition<ProcessDefinition> {
    private final Processor processor;

    public ProcessDefinition(Processor processor) {

        this.processor = processor;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        return processor;
    }
}
