package com.nxttxn.vramel.model;

import com.nxttxn.vramel.processor.DynamicRouter;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.spi.FlowContext;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicRouterDefinition<T extends ProcessorDefinition<T>> extends NoOutputExpressionNode {
    public DynamicRouterDefinition(Expression expression) {
        super(expression);
    }

    @Override
    public Processor createProcessor(FlowContext flowContext) throws Exception {
        Expression expression = getExpression().createExpression(flowContext);

        DynamicRouter dynamicRouter = new DynamicRouter(flowContext.getVramelContext(), expression, ",");

        return dynamicRouter;
    }
}
