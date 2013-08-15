package com.nxttxn.vramel.predicates;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.AsyncProcessor;
import com.nxttxn.vramel.Processor;
import com.nxttxn.vramel.processor.async.OptionalAsyncResultHandler;

/**
 * Created with IntelliJ IDEA.
 * User: chuck
 * Date: 7/30/13
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractSignatureBaseStringValidator extends SignatureBaseStringValidationSupport implements Processor {
    public AbstractSignatureBaseStringValidator(String secret) {
        super(secret);
    }



    @Override
    public void process(Exchange exchange) throws Exception {

        final boolean isValid = isValid(getSignature(exchange), getSignatureBaseString(exchange));

        if (!isValid) {
            exchange.setException(getException());
        }
    }

    protected abstract Throwable getException();

    protected abstract String getSignatureBaseString(Exchange exchange);

    protected abstract String getSignature(Exchange exchange);
}
