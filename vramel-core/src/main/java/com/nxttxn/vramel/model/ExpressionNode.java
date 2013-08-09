package com.nxttxn.vramel.model;

import com.google.common.collect.Lists;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.model.language.ExpressionDefinition;
import com.nxttxn.vramel.processor.FilterProcessor;
import com.nxttxn.vramel.spi.FlowContext;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/25/13
 * Time: 11:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionNode extends ProcessorDefinition<ExpressionNode>{
    private ExpressionDefinition expression;
    private List<ProcessorDefinition<?>> outputs = Lists.newArrayList();
    public ExpressionNode(ExpressionDefinition expression) {
        this.expression = expression;
    }

    public ExpressionNode(Expression expression) {
        if (expression != null) {
            setExpression(ExpressionNodeHelper.toExpressionDefinition(expression));
        }
    }
    public ExpressionNode(Predicate predicate) {
        if (predicate != null) {
            setExpression(ExpressionNodeHelper.toExpressionDefinition(predicate));
        }
    }

    public ExpressionNode() {
    }

    @Override
    public List<ProcessorDefinition<?>> getOutputs() {
        return outputs;
    }

    protected FilterProcessor createFilterProcessor(FlowContext flowContext) throws Exception {
        Processor childProcessor = this.createChildProcessor(flowContext);
        return new FilterProcessor(createPredicate(flowContext), childProcessor);
    }


    private com.nxttxn.vramel.Predicate createPredicate(FlowContext flowContext) {
        return getExpression().createPredicate(flowContext);
    }

    public ExpressionDefinition getExpression() {
        return expression;
    }

    public void setExpression(ExpressionDefinition expression) {
        this.expression = expression;
    }
}
