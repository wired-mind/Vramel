package com.nxttxn.vramel.model;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/3/13
 * Time: 10:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class CatchDefinition extends ProcessorDefinition<CatchDefinition> {
    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        throw new UnsupportedOperationException("not implemented");
    }
}
