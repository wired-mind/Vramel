package com.nxttxn.vramel.processor;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Processor;
import org.slf4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 6/26/13
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class LogProcessor implements Processor {
    private final Expression expression;
    private final Logger logger;

    public LogProcessor(Expression expression, Logger logger) {
        this.expression = expression;
        this.logger = logger;
    }

    public void process(Exchange exchange) throws Exception {
        final String msg = expression.evaluate(exchange, String.class);
        logger.info(msg);
    }

}
