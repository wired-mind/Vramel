package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/19/13
 * Time: 9:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class OutputDefinition<T extends ProcessorDefinition<T>> extends ProcessorDefinition<T> {
    private List<ProcessorDefinition<?>> outputs = Lists.newArrayList();

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return outputs;
    }
}
