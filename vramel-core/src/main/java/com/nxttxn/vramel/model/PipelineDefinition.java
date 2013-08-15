package com.nxttxn.vramel.model;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 10:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class PipelineDefinition extends OutputDefinition<PipelineDefinition>{

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        return createChildProcessor(flowContext);
    }


}
