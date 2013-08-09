package com.nxttxn.vramel.model;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoOutputDefinition<T extends ProcessorDefinition<T>> extends ProcessorDefinition<T> {
    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return Collections.emptyList();
    }
}
