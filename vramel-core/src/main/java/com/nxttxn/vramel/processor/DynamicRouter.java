package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.VramelContext;
import com.nxttxn.vramel.util.ObjectHelper;

import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/12/13
 * Time: 10:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class DynamicRouter extends RoutingSlip {

    public DynamicRouter(VramelContext vramelContext) {
        super(vramelContext);
    }

    public DynamicRouter(VramelContext vramelContext, Expression expression, String uriDelimiter) {
        super(vramelContext, expression, uriDelimiter);
    }

    @Override
    protected RoutingSlipIterator createRoutingSlipIterator(Exchange exchange) throws Exception {
        return new DynamicRoutingSlipIterator(expression);
    }

    /**
     * The dynamic routing slip iterator.
     */
    private final class DynamicRoutingSlipIterator implements RoutingSlipIterator {

        private final Expression slip;
        private Iterator<?> current;

        private DynamicRoutingSlipIterator(Expression slip) {
            this.slip = slip;
        }

        public boolean hasNext(Exchange exchange) {
            if (current != null && current.hasNext()) {
                return true;
            }
            // evaluate next slip
            Object routingSlip = slip.evaluate(exchange, Object.class);
            if (routingSlip == null) {
                return false;
            }
            current = ObjectHelper.createIterator(routingSlip, uriDelimiter);
            return current != null && current.hasNext();
        }

        public Object next(Exchange exchange) {
            return current.next();
        }
    }
}
