package com.nxttxn.vramel.model;

import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.MarshalProcessor;
import com.nxttxn.vramel.spi.DataFormat;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/18/13
 * Time: 3:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class MarshalDefinition extends NoOutputDefinition<MarshalDefinition> {
    private final DataFormatDefinition dataFormatType;

    public MarshalDefinition(DataFormatDefinition dataFormatType) {
        this.dataFormatType = dataFormatType;
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        DataFormat dataFormat = dataFormatType.getDataFormat(flowContext);
        return new MarshalProcessor(dataFormat);
    }
}
