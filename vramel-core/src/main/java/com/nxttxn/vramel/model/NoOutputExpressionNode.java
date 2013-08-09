package com.nxttxn.vramel.model;

import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.model.language.ExpressionDefinition;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoOutputExpressionNode extends ExpressionNode{
    public NoOutputExpressionNode() {
    }

    public NoOutputExpressionNode(ExpressionDefinition expression) {
        super(expression);
    }

    public NoOutputExpressionNode(Expression expression) {
        super(expression);
    }

    public NoOutputExpressionNode(Predicate predicate) {
        super(predicate);
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return Collections.emptyList();
    }


    @Override
    public void addOutput(ProcessorDefinition<?> output) {
        // add it to the parent as we do not support outputs
        getParent().addOutput(output);
    }
}
