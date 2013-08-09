package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/14/13
 * Time: 2:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterDefinition extends ExpressionNode {

    public FilterDefinition(Predicate predicate) {
        super(predicate);
    }

    public FilterDefinition() {

    }


    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        return createFilterProcessor(flowContext);
    }
}
